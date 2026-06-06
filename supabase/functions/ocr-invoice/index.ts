import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { OcrInputSchema, OcrResponseSchema } from './validators.ts'
import { callGemini, GEMINI_1_5_FLASH } from '../_shared/gemini.ts'
import { INVOICE_OCR_PROMPT } from '../_shared/prompts.ts'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
}

serve(async (req) => {
  // 1. Handle CORS Preflight Requests
  if (req.method === 'OPTIONS') {
    return new Response('ok', { headers: corsHeaders })
  }

  // 2. Enforce POST requests only
  if (req.method !== 'POST') {
    return new Response(
      JSON.stringify({ error: `Method ${req.method} not allowed. Use POST.` }),
      { status: 405, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // Retrieve environment variables
  const supabaseUrl = Deno.env.get('SUPABASE_URL')
  const supabaseAnonKey = Deno.env.get('SUPABASE_ANON_KEY')
  const supabaseServiceRoleKey = Deno.env.get('SUPABASE_SERVICE_ROLE_KEY')
  const geminiApiKey = Deno.env.get('GEMINI_API_KEY')

  if (!supabaseUrl || !supabaseAnonKey || !supabaseServiceRoleKey || !geminiApiKey) {
    console.error("Missing required environment configuration variables.")
    return new Response(
      JSON.stringify({ error: "Server misconfiguration. Environment variables missing." }),
      { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // 3. Authenticate User JWT
  const authHeader = req.headers.get('Authorization')
  if (!authHeader) {
    return new Response(
      JSON.stringify({ error: "Missing Authorization header token." }),
      { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  const userClient = createClient(supabaseUrl, supabaseAnonKey, {
    global: { headers: { Authorization: authHeader } }
  })

  const { data: { user }, error: authError } = await userClient.auth.getUser()
  if (authError || !user) {
    console.warn("JWT token authentication failure:", authError?.message)
    return new Response(
      JSON.stringify({ error: "Unauthorized. Invalid JWT session token." }),
      { status: 401, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // Instantiate administrative client to bypass RLS for quota validation and logging
  const adminClient = createClient(supabaseUrl, supabaseServiceRoleKey)

  // 4. Resolve Store Context
  const { data: profile, error: profileError } = await adminClient
    .from('profiles')
    .select('store_id')
    .eq('id', user.id)
    .single()

  if (profileError || !profile) {
    console.error(`Profile lookup failed for user ID ${user.id}:`, profileError?.message)
    return new Response(
      JSON.stringify({ error: "User store profile context not found." }),
      { status: 404, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }
  const storeId = profile.store_id

  // 5. Parse and Validate Request Payload
  let bodyStr = ""
  let bodyData: any
  try {
    bodyStr = await req.text()
    bodyData = JSON.parse(bodyStr)
  } catch (e) {
    return new Response(
      JSON.stringify({ error: "Malformed request payload. JSON expected." }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  const inputValidation = OcrInputSchema.safeParse(bodyData)
  if (!inputValidation.success) {
    return new Response(
      JSON.stringify({ error: "Invalid request inputs.", details: inputValidation.error.format() }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  const { image_path: imagePath, bucket_name: bucketName, idempotency_key: idempotencyKey } = inputValidation.data

  // 6. Idempotency Check (Retry-Safe Architecture)
  const { data: existingJob, error: jobFetchError } = await adminClient
    .from('ocr_jobs')
    .select('*')
    .eq('idempotency_key', idempotencyKey)
    .maybeSingle()

  if (jobFetchError) {
    console.error("Database query failed while fetching idempotency key:", jobFetchError.message)
  }

  if (existingJob) {
    if (existingJob.status === 'completed') {
      console.log(`Idempotency hit! Returning cached results for key: ${idempotencyKey}`)
      return new Response(
        JSON.stringify(existingJob.raw_response),
        { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }

    if (existingJob.status === 'processing') {
      const jobAge = Date.now() - new Date(existingJob.updated_at).getTime()
      if (jobAge < 300000) { // 5 minutes timeout limit for old jobs
        console.warn(`Idempotency hit: job is currently processing and active. Key: ${idempotencyKey}`)
        return new Response(
          JSON.stringify({ error: "Request is currently processing. Please retry in a few moments." }),
          { status: 409, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
        )
      }
      console.log(`Stale processing job found for key ${idempotencyKey}. Re-attempting processing...`)
    }
  }

  // 7. AI Cost Protection: Validate Quota & Rate Limit (Max 20 scans per store/day)
  const payloadSize = new Blob([bodyStr]).size;
  const { data: quotaCheck, error: quotaRpcError } = await adminClient.rpc(
    'check_and_increment_ai_quota',
    {
      p_store_id: storeId,
      p_endpoint: 'ocr-invoice',
      p_max_daily: 20, // 20 scans limit per store/day
      p_payload_size_bytes: payloadSize,
      p_max_payload_size: 10485760 // 10 MB limit
    }
  );

  if (quotaRpcError) {
    console.error("Quota validation RPC invocation failed:", quotaRpcError.message)
  }

  if (quotaCheck && !quotaCheck.allowed) {
    console.warn(`[Quota Blocked] Store: ${storeId}, Endpoint: ocr-invoice. Reason: ${quotaCheck.reason}`)
    return new Response(
      JSON.stringify({ error: "AI Limit Exhausted", details: quotaCheck.message }),
      { status: 429, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // 8. Log the Initial Job State (Transition to Processing)
  let currentJobId = existingJob?.id
  const attemptCount = (existingJob?.attempt_count ?? 0) + 1

  if (existingJob) {
    const { error: updateError } = await adminClient
      .from('ocr_jobs')
      .update({
        status: 'processing',
        attempt_count: attemptCount,
        last_attempted_at: new Date().toISOString()
      })
      .eq('id', currentJobId)

    if (updateError) {
      console.error("Failed to update status on existing ocr_job:", updateError.message)
    }
  } else {
    const { data: newJob, error: insertError } = await adminClient
      .from('ocr_jobs')
      .insert({
        store_id: storeId,
        job_type: 'invoice',
        status: 'processing',
        image_path: imagePath,
        bucket_name: bucketName,
        idempotency_key: idempotencyKey,
        attempt_count: 1,
        last_attempted_at: new Date().toISOString()
      })
      .select('id')
      .single()

    if (insertError || !newJob) {
      console.error("Failed logging new ocr_jobs row:", insertError?.message)
      return new Response(
        JSON.stringify({ error: "Database error initializing OCR job audit logging." }),
        { status: 500, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }
    currentJobId = newJob.id
  }

  // Helper cleanup logging function
  const markJobFailed = async (errorMessage: string) => {
    await adminClient
      .from('ocr_jobs')
      .update({
        status: 'failed',
        error_message: errorMessage
      })
      .eq('id', currentJobId)
  }

  // 9. Fetch Image from storage bucket
  console.log(`Downloading file from storage bucket: ${bucketName}, path: ${imagePath}`)
  const { data: fileData, error: downloadError } = await adminClient
    .storage
    .from(bucketName)
    .download(imagePath)

  if (downloadError || !fileData) {
    const errMsg = `Supabase Storage download failure: ${downloadError?.message ?? 'Empty file content returned'}`
    console.error(errMsg)
    await markJobFailed(errMsg)
    return new Response(
      JSON.stringify({ error: "Failed to download target invoice image from storage." }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // Get MIME type based on file extension
  const fileExt = imagePath.split('.').pop()?.toLowerCase() ?? 'jpeg'
  const mimeType = fileExt === 'png' ? 'image/png'
                 : fileExt === 'webp' ? 'image/webp'
                 : (fileExt === 'heic' || fileExt === 'heif') ? 'image/heic'
                 : 'image/jpeg'

  // Convert downloaded ArrayBuffer data to Base64 format
  const arrayBuffer = await fileData.arrayBuffer()
  const uint8Array = new Uint8Array(arrayBuffer)
  let binaryString = ""
  for (let i = 0; i < uint8Array.byteLength; i++) {
    binaryString += String.fromCharCode(uint8Array[i])
  }
  const base64Data = btoa(binaryString)

  // 10. Execute Gemini 1.5 Flash Vision API call with strict timeouts & retry policy
  let finalStructuredData: any
  let usedFallback = false

  try {
    const contents = [
      {
        parts: [
          { text: INVOICE_OCR_PROMPT },
          {
            inlineData: {
              mimeType: mimeType,
              data: base64Data
            }
          }
        ]
      }
    ]

    finalStructuredData = await callGemini(geminiApiKey, contents, {
      model: GEMINI_1_5_FLASH,
      temperature: 0.1,
      topP: 0.8,
      zodSchema: OcrResponseSchema,
      timeoutMs: 22000, // Strict 22 seconds timeout (satisfying Recommendation 5: 15–25s)
      maxRetries: 2
    })
  } catch (error: any) {
    console.warn("Gemini Vision processing timed out or failed. Activating partial recovery fallback. Error:", error.message)
    // Local Graceful Fallback: Return empty items so user can input manually without the queue blocking or crashing
    finalStructuredData = {
      invoice_number: "UNKNOWN",
      invoice_date: null,
      total_amount: 0.0,
      tax_amount: 0.0,
      items: []
    }
    usedFallback = true
  }

  // Ensure items structure exists
  if (!finalStructuredData || !Array.isArray(finalStructuredData.items)) {
    finalStructuredData = {
      invoice_number: "UNKNOWN",
      invoice_date: null,
      total_amount: 0.0,
      tax_amount: 0.0,
      items: []
    }
    usedFallback = true
  }

  const itemsExtractedCount = finalStructuredData.items.length

  // Inject meta parameters
  finalStructuredData._meta = {
    fallback: usedFallback,
    quota_remaining: quotaCheck?.remaining ?? -1
  }

  // 12. Log Success Status & Increment monthly limit counter
  const { error: finalJobUpdateError } = await adminClient
    .from('ocr_jobs')
    .update({
      status: usedFallback ? 'failed' : 'completed',
      ai_model_used: 'gemini-1.5-flash',
      items_extracted: itemsExtractedCount,
      raw_response: finalStructuredData,
      error_message: usedFallback ? 'Gemini API call timed out or failed to return valid items' : null,
      updated_at: new Date().toISOString()
    })
    .eq('id', currentJobId)

  if (finalJobUpdateError) {
    console.error("Failed to commit final completed ocr_job details:", finalJobUpdateError.message)
  }

  const { error: counterIncrementError } = await adminClient
    .rpc('increment_ocr_counter', { p_store_id: storeId })

  if (counterIncrementError) {
    console.error("Failed calling increment_ocr_counter RPC:", counterIncrementError.message)
  }

  console.log(`Successfully completed OCR job ${currentJobId}. Extracted ${itemsExtractedCount} items. (Fallback: ${usedFallback})`)

  return new Response(
    JSON.stringify(finalStructuredData),
    { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
  )
})
