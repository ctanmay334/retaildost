package com.example.data.dao

import androidx.room.Dao
import androidx.room.Query

data class ItemQuantitySold(
    val itemName: String,
    val totalSold: Double
)

@Dao
interface AnalyticsDao {

    @Query("SELECT SUM(totalAmount) FROM sale_records WHERE createdAt >= :cutoffMillis")
    suspend fun getMonthlyRevenue(cutoffMillis: Long): Double?

    @Query("SELECT SUM(runningBalance) FROM khata_customers WHERE runningBalance > 0")
    suspend fun getKhataOutstanding(): Double?

    @Query("SELECT COUNT(*) FROM inventory WHERE quantity <= minThreshold")
    suspend fun getLowStockCount(): Int

    @Query("SELECT COUNT(*) FROM inventory WHERE expiryDate IS NOT NULL AND expiryDate != '' AND expiryDate <= :cutoffDate")
    suspend fun getExpiryRiskCount(cutoffDate: String): Int

    @Query("""
        SELECT itemName, SUM(quantitySold) as totalSold 
        FROM sale_record_items 
        GROUP BY itemName 
        ORDER BY totalSold DESC 
        LIMIT :limit
    """)
    suspend fun getFastestMovingProducts(limit: Int): List<ItemQuantitySold>
}
