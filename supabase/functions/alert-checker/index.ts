import { serve } from "https://deno.land/std@0.168.0/http/server.ts"
import { createClient } from "https://esm.sh/@supabase/supabase-js@2"

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
}

// Interface for FCM JWT signature payload
interface GoogleAuthPayload {
  iss: string
  scope: string
  aud: string
  exp: number
  iat: number
}

/**
 * Exchange Firebase Service Account JSON for an OAuth2 Access Token using WebCrypto (dependency-free)
 */
async function getFcmAccessToken(serviceAccountJsonStr: string): Promise<string> {
  const serviceAccount = JSON.parse(serviceAccountJsonStr)
  const clientEmail = serviceAccount.client_email
  const privateKeyPem = serviceAccount.private_key

  // Extract raw DER bytes from the PEM private key
  const pemHeader = "-----BEGIN PRIVATE KEY-----"
  const pemFooter = "-----END PRIVATE KEY-----"
  const pemContents = privateKeyPem
    .replace(pemHeader, "")
    .replace(pemFooter, "")
    .replace(/\s+/g, "")

  const binaryDerString = atob(pemContents)
  const binaryDer = new Uint8Array(binaryDerString.length)
  for (let i = 0; i < binaryDerString.length; i++) {
    binaryDer[i] = binaryDerString.charCodeAt(i)
  }

  // Import private key using WebCrypto
  const cryptoKey = await crypto.subtle.importKey(
    "pkcs8",
    binaryDer.buffer,
    {
      name: "RSASSA-PKCS1-v1_5",
      hash: "SHA-256",
    },
    false,
    ["sign"]
  )

  // Construct JWT Header & Payload
  const header = { alg: "RS256", typ: "JWT" }
  const now = Math.floor(Date.now() / 1000)
  const payload: GoogleAuthPayload = {
    iss: clientEmail,
    scope: "https://www.googleapis.com/auth/firebase.messaging",
    aud: "https://oauth2.googleapis.com/token",
    exp: now + 3600,
    iat: now,
  }

  const encoder = new TextEncoder()
  const encodedHeader = btoa(JSON.stringify(header)).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_")
  const encodedPayload = btoa(JSON.stringify(payload)).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_")
  const stringToSign = `${encodedHeader}.${encodedPayload}`

  // Sign JWT using RS256
  const signatureBuffer = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    cryptoKey,
    encoder.encode(stringToSign)
  )

  const signatureArray = new Uint8Array(signatureBuffer)
  let signatureString = ""
  for (let i = 0; i < signatureArray.length; i++) {
    signatureString += String.fromCharCode(signatureArray[i])
  }
  const encodedSignature = btoa(signatureString).replace(/=/g, "").replace(/\+/g, "-").replace(/\//g, "_")

  const jwt = `${stringToSign}.${encodedSignature}`

  // Exchange signed assertion for access token
  const tokenResponse = await fetch("https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: {
      "Content-Type": "application/x-www-form-urlencoded",
    },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion: jwt,
    }),
  })

  if (!tokenResponse.ok) {
    const errorText = await tokenResponse.text()
    throw new Error(`Google OAuth token exchange failed: ${errorText}`)
  }

  const tokenData = await tokenResponse.json()
  return tokenData.access_token
}

serve(async (req) => {
  // Handle CORS
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders })
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL")
  const supabaseServiceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY")

  if (!supabaseUrl || !supabaseServiceRoleKey) {
    console.error("Missing DB environment variables")
    return new Response(
      JSON.stringify({ error: "Missing DB configuration on server." }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }

  // 1. Verify Authorization (Require Service Role or Cron trigger)
  const authHeader = req.headers.get("Authorization")
  if (!authHeader || !authHeader.startsWith("Bearer ")) {
    return new Response(
      JSON.stringify({ error: "Unauthorized. Missing bearer token." }),
      { status: 401, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }

  const token = authHeader.substring(7)
  if (token !== supabaseServiceRoleKey) {
    console.warn("Unauthorized attempt to invoke alert-checker")
    return new Response(
      JSON.stringify({ error: "Unauthorized. Service role required." }),
      { status: 403, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }

  // Instantiate administrative database client
  const adminClient = createClient(supabaseUrl, supabaseServiceRoleKey)

  try {
    console.log("Starting daily alert generation scan...")
    // 2. Call DB function to evaluate rules and insert alerts
    const { data: generatedAlerts, error: rpcError } = await adminClient.rpc("generate_daily_alerts")

    if (rpcError) {
      console.error("Failed to run generate_daily_alerts RPC:", rpcError.message)
      throw rpcError
    }

    const alertsList = generatedAlerts || []
    console.log(`Generated ${alertsList.length} new alerts.`)

    if (alertsList.length === 0) {
      return new Response(
        JSON.stringify({ message: "Alert scan completed. No new alerts generated.", count: 0 }),
        { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
      )
    }

    // 3. Setup FCM integration if credentials are present
    const fcmServiceAccount = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON")
    let fcmToken: string | null = null
    let projectId = ""

    if (fcmServiceAccount) {
      try {
        fcmToken = await getFcmAccessToken(fcmServiceAccount)
        projectId = JSON.parse(fcmServiceAccount).project_id
        console.log(`Successfully authenticated FCM. Target Project ID: ${projectId}`)
      } catch (err: any) {
        console.error("FCM Token authentication error:", err.message)
      }
    } else {
      console.warn("FIREBASE_SERVICE_ACCOUNT_JSON is not configured. Skipping FCM push dispatches.")
    }

    const sentAlertIds: string[] = []

    // 4. Send notifications for each alert with a valid token
    if (fcmToken && projectId) {
      const sendPromises = alertsList.map(async (alert: any) => {
        if (!alert.fcm_token) {
          console.log(`Alert ${alert.alert_id} has no registered FCM token. Skipping push.`)
          return
        }

        // Construct production-grade FCM V1 Payload supporting deep links
        const fcmPayload = {
          message: {
            token: alert.fcm_token,
            notification: {
              title: alert.alert_type === "low_stock" ? "⚠️ Low Stock Alert" : "⏰ Expiry Warning",
              body: alert.message,
            },
            data: {
              click_action: "retaildost://alerts",
              type: alert.alert_type,
              inventory_id: alert.inventory_id || "",
              item_name: alert.item_name,
              deep_link: `retaildost://inventory/detail?id=${alert.inventory_id}`,
            },
            android: {
              priority: "high",
              notification: {
                click_action: `retaildost://inventory/detail?id=${alert.inventory_id}`,
                sound: "default",
                channel_id: "retaildost_alerts_channel",
              },
            },
          },
        }

        try {
          const response = await fetch(
            `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
            {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${fcmToken}`,
              },
              body: JSON.stringify(fcmPayload),
            }
          )

          if (response.ok) {
            sentAlertIds.push(alert.alert_id)
            console.log(`FCM push sent successfully for Alert ID: ${alert.alert_id}`)
          } else {
            const errorDetails = await response.text()
            console.error(`FCM sending returned error for Alert ${alert.alert_id}:`, errorDetails)
          }
        } catch (err: any) {
          console.error(`Failed to dispatch FCM push for Alert ${alert.alert_id}:`, err.message)
        }
      })

      // Await all notification dispatches
      await Promise.all(sendPromises)

      // 5. Update FCM delivery status in the database for successful dispatches
      if (sentAlertIds.length > 0) {
        const { error: updateError } = await adminClient
          .from("alerts")
          .update({
            fcm_sent: true,
            fcm_sent_at: new Date().toISOString(),
          })
          .in("id", sentAlertIds)

        if (updateError) {
          console.error("Failed to update fcm_sent flags in DB:", updateError.message)
        } else {
          console.log(`Flagged ${sentAlertIds.length} alerts as FCM sent in DB.`)
        }
      }
    }

    return new Response(
      JSON.stringify({
        message: "Alert check complete.",
        total_alerts: alertsList.length,
        pushes_attempted: alertsList.filter((a: any) => a.fcm_token).length,
        pushes_sent: sentAlertIds.length,
        alerts: alertsList.map((a: any) => ({
          id: a.alert_id,
          item: a.item_name,
          type: a.alert_type,
          sent: sentAlertIds.includes(a.alert_id),
        })),
      }),
      { status: 200, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )

  } catch (error: any) {
    console.error("Critical error in alert-checker invocation:", error.message)
    return new Response(
      JSON.stringify({ error: "Internal Server Error in alert execution.", details: error.message }),
      { status: 500, headers: { ...corsHeaders, "Content-Type": "application/json" } }
    )
  }
})
