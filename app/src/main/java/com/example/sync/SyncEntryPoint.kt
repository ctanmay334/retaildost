package com.example.sync

import com.example.data.repository.InventoryRepository
import com.example.data.repository.KhataRepository
import com.example.data.repository.OfflineQueueRepository
import com.example.data.repository.SaleRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * SyncEntryPoint
 * ──────────────
 * Custom Hilt EntryPoint allowing background workers (like SyncWorker) which are instantiated
 * by Android WorkManager to fetch injected repositories from the application context safely.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface SyncEntryPoint {
    fun inventoryRepository(): InventoryRepository
    fun saleRepository(): SaleRepository
    fun khataRepository(): KhataRepository
    fun offlineQueueRepository(): OfflineQueueRepository
    fun ocrScannerRepository(): com.example.data.repository.OcrScannerRepository
}
