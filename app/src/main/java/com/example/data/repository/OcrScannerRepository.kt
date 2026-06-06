package com.example.data.repository

import android.content.Context
import android.net.Uri
import com.example.data.model.InventoryEntity

/**
 * OcrScannerRepository
 * ────────────────────
 * Repository managing captured invoice photo processing, local compression scaling,
 * remote Supabase Storage uploads, and AI Gemini OCR parsing with intelligent offline caching.
 */
interface OcrScannerRepository {

    /**
     * Executes the complete invoice scan flow:
     * 1. Resizes and quality compresses captured image below 300KB.
     * 2. If online: uploads to Supabase storage -> calls OCR generation API -> returns parsed inventory items.
     * 3. If offline: stores image on disk -> enqueues sync task -> throws OfflineQueueException.
     */
    suspend fun processInvoiceScan(
        context: Context,
        imageUri: Uri,
        isHandwritten: Boolean = false
    ): Result<List<InventoryEntity>>

    /**
     * Drains the 'offline_queue' for "invoice_ocr_sync" tasks,
     * processing compressed disk-cached images, uploading them, running OCR parsing,
     * and committing the resolved products into the inventory repositories.
     */
    suspend fun syncPendingOcrScans(context: Context): Result<Unit>
}
