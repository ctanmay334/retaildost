import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from 'https://esm.sh/@supabase/supabase-js@2'
import { KhataNlpInputSchema, KhataNlpResponseSchema } from './validators.ts'
import { callGemini, GEMINI_1_5_FLASH } from '../_shared/gemini.ts'
import { KHATA_INTENT_PROMPT } from '../_shared/prompts.ts'

const corsHeaders = {
  'Access-Control-Allow-Origin': '*',
  'Access-Control-Allow-Headers': 'authorization, x-client-info, apikey, content-type',
  'Access-Control-Allow-Methods': 'POST, OPTIONS',
}

/**
 * Fallback Local Heuristic Parser
 * Used to ensure graceful degradation and zero downtime in case of Gemini timeout/outage.
 */
function localHeuristicFallback(rawInput: string): any {
  const lower = rawInput.toLowerCase();
  
  // 1. Detect Intent Heuristics
  let intent = "unknown";
  
  if (lower.includes("balance") || lower.includes("hisab") || lower.includes("query") || lower.includes("status") || lower.includes("kitna") || lower.includes("batao")) {
    intent = "query";
  } else {
    // Check specific Hinglish phrases for Suresh/credit/debit
    const hasSe = lower.includes("se");
    const hasKo = lower.includes("ko");
    const hasNe = lower.includes("ne");
    const hasLiye = lower.includes("liye") || lower.includes("liya");
    const hasDiya = lower.includes("diya") || lower.includes("diye");
    const hasMila = lower.includes("mila");
    const hasUdhar = lower.includes("udhar") || lower.includes("udhaar");

    if (hasSe && hasLiye) {
      intent = "credit"; // Suresh se 500 liye -> Suresh gave (credit)
    } else if (hasNe && hasDiya) {
      intent = "credit"; // Suresh ne 500 diye/diya -> Suresh gave (credit)
    } else if (hasSe && hasUdhar && hasLiye) {
      intent = "credit"; // Suresh se udhar liya -> Suresh lended (credit)
    } else if (hasSe && hasMila) {
      intent = "credit"; // Suresh se 150 mila -> received (credit)
    } else if (hasKo && hasDiya) {
      intent = "debit"; // Suresh ko 500 diya -> gave Suresh (debit)
    } else if (hasKo && hasUdhar) {
      intent = "debit"; // Suresh ko udhar diya -> gave Suresh (debit)
    } else if (lower.includes("gave") || lower.includes("dena") || hasUdhar || lower.includes("debit") || lower.includes("due")) {
      intent = "debit";
    } else if (lower.includes("got") || lower.includes("jama") || lower.includes("received") || lower.includes("credit") || lower.includes("pay")) {
      intent = "credit";
    }
  }

  // 2. Parse Amount Heuristics (Extract numeric strings near currency keywords)
  const amountMatch = rawInput.match(/(?:rs\.?|rupees|₹|\bval\b)\s*(\d+(?:\.\d+)?)|(\d+(?:\.\d+)?)\s*(?:rs\.?|rupees|₹)/i);
  let amount = 0.0;
  if (amountMatch) {
    amount = parseFloat(amountMatch[1] || amountMatch[2]);
  } else {
    const generalNumMatch = rawInput.match(/\b(\d+(?:\.\d+)?)\b/);
    if (generalNumMatch) {
      amount = parseFloat(generalNumMatch[1]);
    }
  }

  // 3. Parse Customer Name Heuristics (Hinglish preposition matching in TypeScript)
  let customerName = "Unknown";
  const words = rawInput.trim().split(/\s+/);
  const nameWords: string[] = [];

  const markers = ["ka", "ko", "ne", "se", "pe", "ki", "to", "from", "of", "for"];
  let markerIndex = -1;
  for (let i = 0; i < words.length; i++) {
    if (markers.includes(words[i].toLowerCase())) {
      markerIndex = i;
      break;
    }
  }

  if (markerIndex > 0) {
    for (let i = 0; i < markerIndex; i++) {
      nameWords.push(words[i]);
    }
  } else if (words.length > 0) {
    const firstWord = words[0];
    const invalidNameKeywords = ["udhar", "udhaar", "debit", "credit", "pay", "mila", "diya", "back", "gave", "got", "received", "total", "balance", "hisab"];
    if (isNaN(Number(firstWord)) && !invalidNameKeywords.includes(firstWord.toLowerCase())) {
      nameWords.push(firstWord);
      if (words.length > 1) {
        const secondWord = words[1];
        if (isNaN(Number(secondWord)) && 
            !invalidNameKeywords.includes(secondWord.toLowerCase()) && 
            !markers.includes(secondWord.toLowerCase())) {
          nameWords.push(secondWord);
        }
      }
    }
  }

  if (nameWords.length > 0) {
    customerName = nameWords.map(word => word.charAt(0).toUpperCase() + word.slice(1).toLowerCase()).join(" ");
  }

  return {
    intent,
    customer_name: customerName,
    amount,
    confidence: 0.50 // Moderate confidence marker for fallback results
  };
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

  const inputValidation = KhataNlpInputSchema.safeParse(bodyData)
  if (!inputValidation.success) {
    return new Response(
      JSON.stringify({ error: "Invalid request inputs.", details: inputValidation.error.format() }),
      { status: 400, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  const { raw_input: rawInput, idempotency_key: idempotencyKey } = inputValidation.data

  // 6. Idempotency Check (Retry-Safe Architecture)
  if (idempotencyKey) {
    const { data: existingKey, error: keyFetchError } = await adminClient
      .from('idempotency_keys')
      .select('*')
      .eq('store_id', storeId)
      .eq('idem_key', idempotencyKey)
      .eq('action_type', 'khata_nlp')
      .maybeSingle()

    if (keyFetchError) {
      console.error("Database query failed while fetching idempotency key:", keyFetchError.message)
    }

    if (existingKey) {
      console.log(`Idempotency hit! Returning cached results for key: ${idempotencyKey}`)
      return new Response(
        JSON.stringify(existingKey.response_body),
        { status: existingKey.response_code ?? 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
      )
    }
  }

  // 7. AI Cost Protection: Validate Quota & Rate Limit
  const payloadSize = new Blob([bodyStr]).size;
  const { data: quotaCheck, error: quotaRpcError } = await adminClient.rpc(
    'check_and_increment_ai_quota',
    {
      p_store_id: storeId,
      p_endpoint: 'khata-nlp',
      p_max_daily: 100, // 100 NLP phrases per store/day limit
      p_payload_size_bytes: payloadSize,
      p_max_payload_size: 5120 // 5 KB limit
    }
  );

  if (quotaRpcError) {
    console.error("Quota validation RPC invocation failed:", quotaRpcError.message)
  }

  if (quotaCheck && !quotaCheck.allowed) {
    console.warn(`[Quota Blocked] Store: ${storeId}, Endpoint: khata-nlp. Reason: ${quotaCheck.reason}`)
    return new Response(
      JSON.stringify({ error: "AI Limit Exhausted", details: quotaCheck.message }),
      { status: 429, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
    )
  }

  // 8. Execute Gemini 1.5 Flash API call with tight timeout limit (5-10 seconds)
  let finalStructuredData: any
  let usedFallback = false

  try {
    const contents = [
      {
        parts: [
          { text: `${KHATA_INTENT_PROMPT}\n\nParse the following phrase:\n"${rawInput}"` }
        ]
      }
    ]

    finalStructuredData = await callGemini(geminiApiKey, contents, {
      model: GEMINI_1_5_FLASH,
      temperature: 0.1, // Set to 0.1 per user's IMPORTANT GEMINI RULES request
      topP: 0.8,
      zodSchema: KhataNlpResponseSchema,
      timeoutMs: 7000, // Strict 7-second timeout (satisfying Recommendation 5: 5-10s)
      maxRetries: 2
    })
  } catch (error: any) {
    console.warn("Gemini NLP API invocation timed out or failed. Activating local heuristic fallback. Error:", error.message)
    finalStructuredData = localHeuristicFallback(rawInput)
    usedFallback = true
  }

  // 9. Save to Idempotency Log if key was supplied
  if (idempotencyKey) {
    const { error: insertError } = await adminClient
      .from('idempotency_keys')
      .insert({
        store_id: storeId,
        idem_key: idempotencyKey,
        action_type: 'khata_nlp',
        response_code: 200,
        response_body: finalStructuredData
      })

    if (insertError) {
      console.error(`Failed to insert idempotency key ${idempotencyKey}:`, insertError.message)
    }
  }

  console.log(`[Success] Parsed input: "${rawInput}" -> ${JSON.stringify(finalStructuredData)} (Fallback used: ${usedFallback})`)

  return new Response(
    JSON.stringify({
      ...finalStructuredData,
      _meta: {
        fallback: usedFallback,
        quota_remaining: quotaCheck?.remaining ?? -1
      }
    }),
    { status: 200, headers: { ...corsHeaders, 'Content-Type': 'application/json' } }
  )
})
