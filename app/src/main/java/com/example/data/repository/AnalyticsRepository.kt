package com.example.data.repository

import com.example.data.dao.ItemQuantitySold

data class StoreAnalytics(
    val monthlyRevenue: Double,
    val khataOutstanding: Double,
    val lowStockCount: Int,
    val expiryRiskCount: Int,
    val fastestMovingProducts: List<ItemQuantitySold>,
    val planTier: String
)

interface AnalyticsRepository {
    /**
     * Aggregates local inventory, sales, and khata records to compute real-time KPIs.
     */
    suspend fun getStoreAnalytics(userId: String): Result<StoreAnalytics>

    /**
     * Generates a localized Hinglish summary from Gemini 1.5 Pro with structured caching.
     */
    suspend fun getAiInsights(analytics: StoreAnalytics): Result<String>

    /**
     * Upgrades the specified tenant profile to the Pro tier in both Room and Supabase.
     */
    suspend fun upgradeToProPlan(userId: String): Result<Unit>
}
