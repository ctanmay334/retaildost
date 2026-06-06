package com.example.data.api

import android.graphics.Bitmap
import android.util.Base64
import android.util.Log
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.concurrent.TimeUnit

object GeminiClient {
    private const val TAG = "GeminiClient"
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private const val MODEL_NAME = "gemini-1.5-pro"

    // Helper to convert Bitmap to Base64 Jpeg bytes
    fun bitmapToBase64(bitmap: Bitmap): String {
        val outputStream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, outputStream)
        return Base64.encodeToString(outputStream.toByteArray(), Base64.NO_WRAP)
    }

    /**
     * Parses an invoice image using Gemini OCR. Returns a list of extracted items as JSON.
     */
    suspend fun parseInvoiceOcr(base64Image: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is unconfigured or a placeholder.")
            return@withContext null
        }

        val systemPrompt = """
            You are a specialized OCR extraction engine for Indian FMCG distributor invoices.
            Your ONLY job is to extract structured products data from the invoice image.
            
            CRITICAL RULES:
            - Respond with ONLY a valid raw JSON Array. Do not wrap in ```json ``` or insert markdown.
            - Ensure fields are flat.
            - Fields to extract for each item block:
              - name: product name as printed (e.g., "Aashirvaad Atta 5kg", "Premium Paneer 200g")
              - category: "Staples", "Snacks", "Dairy", "Personal Care" or "Cleaning"
              - brand: brand name (e.g., "ITC", "Amul", "Tata")
              - quantity: integer count of items delivered (e.g., 10, 5)
              - cost_price: unit cost price paid by retailer as double
              - mrp: printed Maximum Retail Price as double
              - expiry_date: "YYYY-MM-DD" if printed or deduce based on category (+6 months for Atta, +1 month for Paneer/Dairy)
              - confidence: double value from 0.0 to 1.0 representing confidence of the extraction
            
            Example output format:
            [
              {"name": "Aashirvaad Atta 5kg", "category": "Staples", "brand": "ITC", "quantity": 10, "costPrice": 240.0, "mrp": 270.0, "expiryDate": "2026-11-27", "confidence": 0.95},
              {"name": "Amul Taaza Milk", "category": "Dairy", "brand": "Amul", "quantity": 25, "costPrice": 26.0, "mrp": 28.0, "expiryDate": "2026-06-05", "confidence": 0.65}
            ]
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemPrompt) })
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
                put("topP", 0.8)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "Raw Gemini Response: $body")

            if (response.isSuccessful) {
                // Parse standard Gemini structure to find candidates[0].content.parts[0].text
                val rootJson = JSONObject(body)
                val candidates = rootJson.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                textResponse
            } else {
                Log.e(TAG, "Unsuccessful response from Gemini: ${response.code} - $body")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calling Gemini API: ${e.message}", e)
            null
        }
    }

    /**
     * Parses spoken or typed Hinglish inputs like "Ramesh ka 500 ka udhar" or "Sunita ne 200 diye"
     * into a structured JSON with custom intent targets.
     */
    suspend fun parseVoiceKhataIntent(rawInput: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is unconfigured or a placeholder.")
            return@withContext null
        }

        val systemPrompt = """
            You are a specialized NLP intent parser for private Indian Retail Kirana credit ledger books (Khata).
            Inputs will be short spoken phrases in Hindi, English or Hinglish (mixed Hindi-English).
            Your ONLY job is to extract customer name, transactions amount, and debit vs credit intent.
            
            INTENTS DEFINITION:
            - debit: Money owed to store. Signal words: "udhar", "udhaar", "baaki", "credit", "diyasaman"
            - credit: Payment received from customer. Signal words: "diye", "waapas", "mila", "payment", "paid"
            
            CRITICAL RULES:
            - Return ONLY a valid JSON object flat. Do not markdown wrap.
            - Format of output:
              {"intent": "debit" | "credit" | "unknown", "customer": "Name", "amount": Double}
            
            Examples:
            - Input: "Ramesh ka 500 ka udhar"
              Output: {"intent": "debit", "customer": "Ramesh", "amount": 500.0}
            - Input: "Sunita ji ne 250 waapas diye"
              Output: {"intent": "credit", "customer": "Sunita Ji", "amount": 250.0}
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemPrompt\nInput text: \"$rawInput\"") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("responseMimeType", "application/json")
                put("temperature", 0.1)
                put("topP", 0.8)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "Raw Gemini Response: $body")

            if (response.isSuccessful) {
                val rootJson = JSONObject(body)
                val candidates = rootJson.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                textResponse
            } else {
                Log.e(TAG, "Unsuccessful response from Gemini: ${response.code} - $body")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing Hinglish search intent: ${e.message}", e)
            null
        }
    }

    /**
     * Generates premium AI-powered store insights in Hinglish (mixed Hindi-English)
     * using Gemini 1.5 Pro, based on actual aggregated store statistics.
     */
    suspend fun generateBusinessInsights(analyticsJson: String): String? = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.GEMINI_API_KEY
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API key is unconfigured or a placeholder.")
            return@withContext null
        }

        val systemPrompt = """
            You are a premier business analytics consultant for Indian retail grocery stores (Kirana shops).
            You will receive a JSON representing the store's current KPI analytics:
            - fastestMovingProducts
            - expiryRiskCount
            - lowStockCount
            - monthlyRevenue
            - khataOutstanding
            
            Your job is to generate a premium business report in Hinglish.
            
            CRITICAL FORMATTING & STYLE RULES:
            - Respond in clear, engaging, and premium HINGLISH (mixed Hindi and English, as spoken naturally in India).
            - Focus on delivering practical, actionable strategies (e.g., how to collect outstanding khata, when to restock low stock items, how to clear expiring items).
            - Format with bold markdown headings, clean bullet points, and neat spacing. Avoid returning raw HTML tags.
            - Start with a "Dukaan Summary" highlighting overall performance.
            - End with 3-4 highly specific "Action Steps" (Kam ki baatein).
            
            Example Hinglish tone:
            "Aapki dukaan ka is mahine ka monthly revenue ₹45,000 hai, which is quite stable! Lekin dhyan rahe, low stock items abhi 8 hain, inko turant restock karne ki zaroorat hai taaki customers khali haath na lautein."
        """.trimIndent()

        val requestJson = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", "$systemPrompt\nStore stats JSON: $analyticsJson") })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.7)
            })
        }

        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = requestJson.toString().toRequestBody(mediaType)

        val url = "https://generativelanguage.googleapis.com/v1beta/models/$MODEL_NAME:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            Log.d(TAG, "Raw Gemini Response: $body")

            if (response.isSuccessful) {
                val rootJson = JSONObject(body)
                val candidates = rootJson.getJSONArray("candidates")
                val textResponse = candidates.getJSONObject(0)
                    .getJSONObject("content")
                    .getJSONArray("parts")
                    .getJSONObject(0)
                    .getString("text")
                textResponse
            } else {
                Log.e(TAG, "Unsuccessful response from Gemini: ${response.code} - $body")
                null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error generating business insights: ${e.message}", e)
            null
        }
    }
}

