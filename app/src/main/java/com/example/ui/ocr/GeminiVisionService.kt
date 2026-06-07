package com.example.ui.ocr

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiVisionService(
    private val apiKey: String
) {
    companion object {
        /**
         * Default model string for invoice data extraction.
         */
        const val MODEL = "gemini-3.5-flash"
        const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)   // Gemini vision on high-res can take up to 90s
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        coerceInputValues = true
    }

    /**
     * MASTER SYSTEM PROMPT — Engineered for Gemini 3.1 Pro.
     */
    private val systemInstruction = """
        You are an expert invoice data extraction AI. Extract every product/item from the invoice image with perfect accuracy.
        
        CRITICAL RULES:
        - Extract EVERY line item — do not skip any product.
        - Use exact product names as printed on the invoice.
        - Quantities: preserve decimals (e.g., 2.5, 0.5). Extract unit separately (kg, pcs, litre, box, dozen, etc.).
        - Prices: extract unit price and total price per line if visible. Numeric values only, no currency symbols.
        - HSN codes: extract if present (Indian GST invoices).
        - If a field is not readable or not present, use null — never guess or fabricate.
        - For Indian invoices: GST, CGST, SGST totals go into tax_amount combined.
        - If image is not a readable invoice, set items to empty array and set supplier_name to "NOT_AN_INVOICE".
    """.trimIndent()

    /**
     * Gemini 3.1 Pro response schema — enforced at API level.
     * This replaces prompt-based JSON enforcement and is more reliable.
     */
    private val responseSchema = JSONObject().apply {
        put("type", "object")
        put("properties", JSONObject().apply {
            put("supplier_name",  JSONObject().apply { put("type", "string")  })
            put("invoice_number", JSONObject().apply { put("type", "string")  })
            put("invoice_date",   JSONObject().apply { put("type", "string")  })
            put("currency",       JSONObject().apply { put("type", "string")  })
            put("subtotal",       JSONObject().apply { put("type", "number")  })
            put("tax_amount",     JSONObject().apply { put("type", "number")  })
            put("total_amount",   JSONObject().apply { put("type", "number")  })
            put("items", JSONObject().apply {
                put("type", "array")
                put("items", JSONObject().apply {
                    put("type", "object")
                    put("properties", JSONObject().apply {
                        put("name",        JSONObject().apply { put("type", "string")  })
                        put("quantity",    JSONObject().apply { put("type", "number")  })
                        put("unit",        JSONObject().apply { put("type", "string")  })
                        put("unit_price",  JSONObject().apply { put("type", "number")  })
                        put("total_price", JSONObject().apply { put("type", "number")  })
                        put("sku",         JSONObject().apply { put("type", "string")  })
                        put("hsn_code",    JSONObject().apply { put("type", "string")  })
                        put("brand",       JSONObject().apply { put("type", "string")  })
                        put("description", JSONObject().apply { put("type", "string")  })
                    })
                    put("required", JSONArray().apply { put("name"); put("quantity") })
                })
            })
        })
        put("required", JSONArray().apply { put("items") })
    }

    suspend fun extractInvoiceData(base64Image: String): Result<ScannedInvoice> =
        withContext(Dispatchers.IO) {
            val modelsToTry = listOf(
                "gemini-3.5-flash",
                "gemini-3.1-flash-lite",
                "gemini-2.5-flash"
            )

            var lastException: Exception? = null

            for (model in modelsToTry) {
                Log.d("GeminiVisionService", "Attempting extraction with model: $model")
                val result = callModelApi(model, base64Image)
                if (result.isSuccess) {
                    Log.i("GeminiVisionService", "Successfully extracted invoice data using model: $model")
                    return@withContext result
                }

                val exception = result.exceptionOrNull() as? Exception
                lastException = exception
                Log.w("GeminiVisionService", "Model $model failed: ${exception?.message}")

                // If it is a bad request/invalid argument (e.g. 400), do not retry other models as they will fail similarly
                if (exception is GeminiApiException && exception.message?.contains("API Error 400", ignoreCase = true) == true) {
                    break
                }
            }

            Result.failure(lastException ?: Exception("Unknown API error"))
        }


    private fun callModelApi(modelName: String, base64Image: String): Result<ScannedInvoice> {
        return try {
            val requestBody = buildRequestJson(base64Image)
            val url = "$BASE_URL/$modelName:generateContent?key=$apiKey"

            val request = Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .post(requestBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = client.newCall(request).execute()
            val responseBody = response.body?.string()

            if (!response.isSuccessful || responseBody == null) {
                return Result.failure(
                    GeminiApiException("API Error ${response.code}: ${responseBody ?: "No response"}")
                )
            }

            val invoice = parseGeminiResponse(responseBody)
            Result.success(invoice)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun buildRequestJson(base64Image: String): JSONObject {
        // System instruction
        val systemInstructionPart = JSONObject().apply {
            put("parts", JSONArray().apply {
                put(JSONObject().apply { put("text", systemInstruction) })
            })
        }

        // Image part — Gemini uses inlineData, not image_url
        val imagePart = JSONObject().apply {
            put("inlineData", JSONObject().apply {
                put("mimeType", "image/jpeg")
                put("data", base64Image)
            })
        }

        // Text part
        val textPart = JSONObject().apply {
            put("text", "Extract all invoice line items and header data from this image.")
        }

        // User content
        val userContent = JSONObject().apply {
            put("role", "user")
            put("parts", JSONArray().apply {
                put(textPart)
                put(imagePart)
            })
        }

        // Generation config — key Gemini 3.1 Pro settings
        val generationConfig = JSONObject().apply {
            put("responseMimeType", "application/json")  // Forces JSON output
            put("responseSchema", responseSchema)         // Schema-enforced structure
            put("temperature", 0.0)                       // Zero temperature = deterministic extraction
            put("maxOutputTokens", 2048)
        }

        return JSONObject().apply {
            put("system_instruction", systemInstructionPart)
            put("contents", JSONArray().apply { put(userContent) })
            put("generationConfig", generationConfig)
        }
    }

    private fun parseGeminiResponse(responseBody: String): ScannedInvoice {
        val responseJson = JSONObject(responseBody)

        // Check for API-level errors
        if (responseJson.has("error")) {
            val error = responseJson.getJSONObject("error")
            throw GeminiApiException("Gemini API error: ${error.optString("message", "Unknown error")}")
        }

        val content = responseJson
            .getJSONArray("candidates")
            .getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()

        val invoice = json.decodeFromString<ScannedInvoice>(content)

        // Check our sentinel value for non-invoice images
        if (invoice.supplierName == "NOT_AN_INVOICE") {
            throw NotAnInvoiceException("The image does not appear to be a readable invoice.")
        }

        return invoice
    }
}

class GeminiApiException(message: String) : Exception(message)
class NotAnInvoiceException(message: String) : Exception(message)
