package com.example.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

private const val TAG = "ImageCompressor"
private const val DEFAULT_MAX_DIMENSION = 1200 // Max edge width/height in pixels
private const val QUALITY_STEP = 10
private const val MIN_QUALITY = 30
private const val COMPRESSION_TARGET_BYTES = 300 * 1024 // 300 KB

/**
 * ImageCompressor
 * ───────────────
 * Production-grade utility to resize, orient, and compress captured invoice scans.
 * Enforces EXIF correction, downscaling boundaries, and progressive JPEG quality scaling
 * to ensure light payload transmissions on high-latency networks.
 */
object ImageCompressor {

    /**
     * Reads a image file via its URI, scales it down, adjusts EXIF rotation,
     * and performs progressive quality compression to output compressed bytes.
     */
    suspend fun compressUriToBytes(
        context: Context,
        uri: Uri,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        targetSizeBytes: Int = COMPRESSION_TARGET_BYTES
    ): ByteArray = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting compression for Uri: $uri")
            
            // 1. Determine image orientation from EXIF metadata
            val rotation = getExifRotation(context, uri)
            Log.d(TAG, "Detected image rotation from EXIF: $rotation degrees")

            // 2. Load and decode bitmap with scale factor
            val bitmap = decodeSampledBitmap(context, uri, maxDimension)
                ?: throw IllegalArgumentException("Unable to decode bitmap from the provided Uri source")

            // 3. Rotate bitmap if required by EXIF metadata
            val orientedBitmap = if (rotation != 0) {
                rotateBitmap(bitmap, rotation)
            } else {
                bitmap
            }

            // 4. Perform progressive quality reduction compression to fit target size
            val compressedBytes = compressBitmapProgressively(orientedBitmap, targetSizeBytes)
            
            // Clean up resources if temporary oriented bitmap was created
            if (orientedBitmap != bitmap) {
                orientedBitmap.recycle()
            }
            bitmap.recycle()

            Log.i(TAG, "Compression complete. Final output size: ${compressedBytes.size / 1024} KB")
            compressedBytes
        } catch (e: Exception) {
            Log.e(TAG, "Failed to compress image Uri source: ${e.message}", e)
            throw e
        }
    }

    /**
     * Resizes and compresses a raw bitmap progressively.
     */
    suspend fun compressBitmapToBytes(
        bitmap: Bitmap,
        maxDimension: Int = DEFAULT_MAX_DIMENSION,
        targetSizeBytes: Int = COMPRESSION_TARGET_BYTES
    ): ByteArray = withContext(Dispatchers.IO) {
        val resized = resizeBitmap(bitmap, maxDimension)
        val bytes = compressBitmapProgressively(resized, targetSizeBytes)
        if (resized != bitmap) {
            resized.recycle()
        }
        bytes
    }

    /**
     * Downscales the source bitmap to fit inside the max dimension boundaries
     * while retaining the original aspect ratio.
     */
    fun resizeBitmap(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height

        if (width <= maxDimension && height <= maxDimension) {
            return source
        }

        val aspectRatio = width.toFloat() / height.toFloat()
        val targetWidth: Int
        val targetHeight: Int

        if (width > height) {
            targetWidth = maxDimension
            targetHeight = (maxDimension / aspectRatio).toInt()
        } else {
            targetHeight = maxDimension
            targetWidth = (maxDimension * aspectRatio).toInt()
        }

        Log.d(TAG, "Resizing bitmap: ${width}x${height} -> ${targetWidth}x${targetHeight}")
        return Bitmap.createScaledBitmap(source, targetWidth, targetHeight, true)
    }

    /**
     * Iteratively encodes the bitmap to JPEG format, reducing compression quality factor
     * until the byte array conforms below [targetSizeBytes].
     */
    private fun compressBitmapProgressively(bitmap: Bitmap, targetSizeBytes: Int): ByteArray {
        var quality = 90
        var outputStream = ByteArrayOutputStream()
        
        bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        Log.d(TAG, "Initial compression at quality=$quality: size=${outputStream.size() / 1024} KB")

        while (outputStream.size() > targetSizeBytes && quality > MIN_QUALITY) {
            quality -= QUALITY_STEP
            outputStream.reset()
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            Log.d(TAG, "Progressive scaling compression quality=$quality: size=${outputStream.size() / 1024} KB")
        }

        return outputStream.toByteArray()
    }

    /**
     * Decodes the source image, performing sub-sampling automatically based on bounds
     * to avoid allocating excessive memory on heap during full size decodes.
     */
    private fun decodeSampledBitmap(context: Context, uri: Uri, maxDimension: Int): Bitmap? {
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        // Read boundaries first
        openInputStream(context, uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        } ?: return null

        // Calculate scale factor
        options.inSampleSize = calculateInSampleSize(options, maxDimension, maxDimension)
        options.inJustDecodeBounds = false

        // Load fully sampled bitmap
        return openInputStream(context, uri)?.use {
            BitmapFactory.decodeStream(it, null, options)
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.outHeight to options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getExifRotation(context: Context, uri: Uri): Int {
        return try {
            openInputStream(context, uri)?.use { inputStream ->
                val exifInterface = ExifInterface(inputStream)
                val orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL
                )
                when (orientation) {
                    ExifInterface.ORIENTATION_ROTATE_90 -> 90
                    ExifInterface.ORIENTATION_ROTATE_180 -> 180
                    ExifInterface.ORIENTATION_ROTATE_270 -> 270
                    else -> 0
                }
            } ?: 0
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract EXIF data: ${e.message}")
            0
        }
    }

    private fun rotateBitmap(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    private fun openInputStream(context: Context, uri: Uri): InputStream? {
        return context.contentResolver.openInputStream(uri)
    }
}
