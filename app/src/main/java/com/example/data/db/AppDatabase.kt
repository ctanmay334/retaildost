package com.example.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.data.dao.*
import com.example.data.model.*

/**
 * AppDatabase
 * ───────────
 * Main database host for offline Room data caching.
 * Registering all legacy and new entities to ensure compatibility.
 */
@Database(
    entities = [
        // Legacy Entities (compatibility)
        ItemEntity::class,
        CustomerEntity::class,
        TransactionEntity::class,
        
        // New Sync-Ready Entities
        ProfileEntity::class,
        InventoryEntity::class,
        SaleRecordEntity::class,
        SaleRecordItemEntity::class,
        KhataCustomerEntity::class,
        KhataTransactionEntity::class,
        AlertEntity::class,
        OfflineQueueEntity::class,
        DistributorEntity::class
    ],
    version = 9,
    exportSchema = false
)
@TypeConverters(com.example.data.db.TypeConverters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun kiranaDao(): KiranaDao
    abstract fun inventoryDao(): InventoryDao
    abstract fun saleDao(): SaleDao
    abstract fun khataDao(): KhataDao
    abstract fun alertDao(): AlertDao
    abstract fun offlineQueueDao(): OfflineQueueDao
    abstract fun distributorDao(): DistributorDao
    abstract fun analyticsDao(): AnalyticsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "kirana_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
