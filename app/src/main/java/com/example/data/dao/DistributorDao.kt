package com.example.data.dao

import androidx.room.*
import com.example.data.model.DistributorEntity
import kotlinx.coroutines.flow.Flow

/**
 * DistributorDao
 * ──────────────
 * Room DAO managing local distributor catalog caching.
 */
@Dao
interface DistributorDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistributor(distributor: DistributorEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDistributors(distributors: List<DistributorEntity>)

    @Query("SELECT * FROM distributors ORDER BY isVerified DESC, businessName ASC")
    fun getAllDistributorsFlow(): Flow<List<DistributorEntity>>

    @Query("SELECT * FROM distributors WHERE category = :category ORDER BY isVerified DESC, businessName ASC")
    fun getDistributorsByCategoryFlow(category: String): Flow<List<DistributorEntity>>

    @Query("DELETE FROM distributors")
    suspend fun clearAllDistributors()
}
