package com.example.data.repository

import android.util.Log
import com.example.data.api.GeminiClient
import com.example.data.dao.AnalyticsDao
import com.example.data.dao.KiranaDao
import com.example.data.model.ProfileEntity
import com.example.data.model.ProfileDto
import com.example.data.model.ProfileUpdate
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import javax.inject.Inject

private const val TAG = "AnalyticsRepositoryImpl"
private const val CACHE_DURATION_MS = 5 * 60 * 1000 // 5 minutes cache

class AnalyticsRepositoryImpl @Inject constructor(
    private val analyticsDao: AnalyticsDao,
    private val kiranaDao: KiranaDao,
    private val supabaseClient: SupabaseClient
) : AnalyticsRepository {

    // Simple in-memory cache for AI Insights to prevent unnecessary Gemini API bills
    private var cachedInsight: String? = null
    private var lastAnalyticsHash: Int = 0
    private var lastInsightTime: Long = 0L

    override suspend fun getStoreAnalytics(userId: String): Result<StoreAnalytics> =
        withContext(Dispatchers.IO) {
            runCatching {
                // 1. Fetch current profile from Room cache
                val profile = kiranaDao.getProfileById(userId)
                val plan = profile?.plan ?: "pro"

                // 2. Monthly Revenue calculation (last 30 days)
                val thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)
                val revenue = analyticsDao.getMonthlyRevenue(thirtyDaysAgo) ?: 0.0

                // 3. Khata Outstanding
                val khataOutstanding = analyticsDao.getKhataOutstanding() ?: 0.0

                // 4. Low Stock Count
                val lowStock = analyticsDao.getLowStockCount()

                // 5. Expiry Risk Count (Expiry within the next 30 days)
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                val cal = Calendar.getInstance()
                cal.add(Calendar.DAY_OF_YEAR, 30)
                val expiryCutoff = sdf.format(cal.time)
                val expiryRisk = analyticsDao.getExpiryRiskCount(expiryCutoff)

                // 6. Fastest-moving products
                val fastestMoving = analyticsDao.getFastestMovingProducts(5)

                StoreAnalytics(
                    monthlyRevenue = revenue,
                    khataOutstanding = khataOutstanding,
                    lowStockCount = lowStock,
                    expiryRiskCount = expiryRisk,
                    fastestMovingProducts = fastestMoving,
                    planTier = plan
                )
            }.onFailure { e ->
                Log.e(TAG, "Error generating store analytics", e)
            }
        }

    override suspend fun getAiInsights(analytics: StoreAnalytics): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                // Determine a unique hash of the analytics metrics to detect changes
                val analyticsHash = analytics.hashCode()
                val now = System.currentTimeMillis()

                if (cachedInsight != null &&
                    analyticsHash == lastAnalyticsHash &&
                    (now - lastInsightTime) < CACHE_DURATION_MS
                ) {
                    Log.d(TAG, "Returning cached AI insights")
                    return@runCatching cachedInsight!!
                }

                // Prepare parameters for Gemini
                val statsJson = JSONObject().apply {
                    put("monthlyRevenue", analytics.monthlyRevenue)
                    put("khataOutstanding", analytics.khataOutstanding)
                    put("lowStockCount", analytics.lowStockCount)
                    put("expiryRiskCount", analytics.expiryRiskCount)
                    
                    val productsArray = org.json.JSONArray()
                    analytics.fastestMovingProducts.forEach {
                        val pObj = JSONObject().apply {
                            put("itemName", it.itemName)
                            put("totalSold", it.totalSold)
                        }
                        productsArray.put(pObj)
                    }
                    put("fastestMovingProducts", productsArray)
                }.toString()

                // Run with a strict 12-second timeout to handle bad network gracefully
                val aiResponse = withTimeoutOrNull(12000) {
                    GeminiClient.generateBusinessInsights(statsJson)
                }

                if (aiResponse != null && aiResponse.isNotBlank()) {
                    cachedInsight = aiResponse
                    lastAnalyticsHash = analyticsHash
                    lastInsightTime = now
                    aiResponse
                } else {
                    // Fail gracefully with a helpful Hinglish backup report
                    Log.w(TAG, "Gemini call timed out or returned empty. Using premium fallback report.")
                    generatePremiumFallbackReport(analytics)
                }
            }.onFailure { e ->
                Log.e(TAG, "Error fetching Gemini insights", e)
            }
        }

    override suspend fun upgradeToProPlan(userId: String): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val profile = kiranaDao.getProfileById(userId)
                    ?: throw IllegalStateException("User profile not found in cache")

                // 1. Persist "pro" plan locally in Room immediately
                val updatedProfile = profile.copy(
                    plan = "pro",
                    updatedAt = System.currentTimeMillis()
                )
                kiranaDao.insertProfile(updatedProfile)
                Log.i(TAG, "Successfully upgraded plan to PRO in local Room DB")

                // 2. Synchronize the plan upgrade to Supabase profiles table
                try {
                    val postgrest = supabaseClient.postgrest
                    val updateMap = mapOf(
                        "plan" to "pro"
                    )
                    postgrest["profiles"].update(updateMap) {
                        filter {
                            eq("id", profile.id)
                        }
                    }
                    Log.i(TAG, "Successfully synchronized plan upgrade to Supabase")
                } catch (e: Exception) {
                    Log.w(TAG, "Supabase profile upgrade sync postponed (offline-first queue will retry)", e)
                }
                Unit
            }.onFailure { e ->
                Log.e(TAG, "Error during plan upgrade to PRO", e)
            }
        }

    private fun generatePremiumFallbackReport(analytics: StoreAnalytics): String {
        return """
            ### 📊 Dukaan Summary (AI Offline Fallback)
            
            Aapka AI connection temporary offline hai, par humare mathematical calculations bilkul ready hain! 
            
            *   **Monthly Revenue**: ₹${String.format("%.2f", analytics.monthlyRevenue)} (Last 30 din ki kamai)
            *   **Khata Outstanding**: ₹${String.format("%.2f", analytics.khataOutstanding)} (Udhari jo vasoolni hai)
            *   **Low Stock Items**: ${analytics.lowStockCount} items low quantity par chal rahe hain.
            *   **Expiry Risk**: ${analytics.expiryRiskCount} items agle 30 dino me expire hone wale hain.
            
            ---
            
            ### 💡 Kam Ki Baatein (Action Steps)
            
            1.  **Low Stock Refill**: Aapki dukaan ke **${analytics.lowStockCount}** items ka stock threshold level se kam hai. Inhe distributor marketplace se contact karke turant restock karein taaki customers khali haath na lautein.
            2.  **Credit Collection**: **₹${String.format("%.2f", analytics.khataOutstanding)}** ka outstanding balance abhi baki hai. WhatsApp payment links bhej kar udhaari ki vasooli shuru karein!
            3.  **Expiry clearance sale**: Agle 30 din me expire hone wale **${analytics.expiryRiskCount}** items par discounts ya Buy-One-Get-One offer chalayein taaki nuksaan se bacha ja sake.
        """.trimIndent()
    }
}
