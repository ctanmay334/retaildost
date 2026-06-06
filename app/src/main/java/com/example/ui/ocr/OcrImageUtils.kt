package com.example.ui.ocr

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.util.Base64
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File

object OcrImageUtils {

    /**
     * Compresses image to max 1024px, fixes EXIF rotation, returns base64 JPEG.
     */
    suspend fun uriToBase64(context: Context, uri: Uri): String = withContext(Dispatchers.IO) {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw IllegalArgumentException("Cannot open image URI: $uri")

        val originalBitmap = BitmapFactory.decodeStream(inputStream)
        inputStream.close()

        val maxDimension = 1024
        val resizedBitmap = if (originalBitmap.width > maxDimension || originalBitmap.height > maxDimension) {
            val ratio = minOf(
                maxDimension.toFloat() / originalBitmap.width,
                maxDimension.toFloat() / originalBitmap.height
            )
            Bitmap.createScaledBitmap(
                originalBitmap,
                (originalBitmap.width * ratio).toInt(),
                (originalBitmap.height * ratio).toInt(),
                true
            )
        } else originalBitmap

        val outputStream = ByteArrayOutputStream()
        resizedBitmap.compress(Bitmap.CompressFormat.JPEG, 87, outputStream)
        val bytes = outputStream.toByteArray()

        val corrected = correctExifRotation(context, uri, bytes)
        Base64.encodeToString(corrected, Base64.NO_WRAP)
    }

    private fun correctExifRotation(context: Context, uri: Uri, bytes: ByteArray): ByteArray {
        return try {
            val exif = context.contentResolver.openInputStream(uri)?.use { ExifInterface(it) } ?: return bytes
            val rotation = when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90  -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotation == 0f) return bytes

            val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
            val matrix = Matrix().apply { postRotate(rotation) }
            val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            val out = ByteArrayOutputStream()
            rotated.compress(Bitmap.CompressFormat.JPEG, 87, out)
            out.toByteArray()
        } catch (e: Exception) {
            bytes // Return original if correction fails — non-fatal
        }
    }

    /**
     * Generates a temporary file Uri under cache directory to pass to camera capture.
     */
    fun createImageUri(context: Context): Uri {
        val tempFile = File.createTempFile("ocr_captured_", ".jpg", context.cacheDir)
        return androidx.core.content.FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            tempFile
        )
    }
}
