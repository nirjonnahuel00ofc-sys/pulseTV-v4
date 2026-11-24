package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Base64
import java.io.ByteArrayOutputStream
import kotlin.math.max

object BannerImageUtils {

    /**
     * Decodes a gallery image Uri selected via Android's zero-permission Photo Picker,
     * downscales it to a crisp banner size (max 960px wide), and compresses it as a
     * lightweight JPEG data URI under 380 KB so it syncs cleanly via Cloud Firestore.
     */
    fun encodeGalleryImageToDataUri(context: Context, uri: Uri): Result<String> {
        return try {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, bounds)
            } ?: return Result.failure(IllegalStateException("Could not open selected image."))

            val srcW = bounds.outWidth
            val srcH = bounds.outHeight
            if (srcW <= 0 || srcH <= 0) {
                return Result.failure(IllegalArgumentException("Unsupported or empty image file."))
            }

            var sampleSize = 1
            while ((srcW / sampleSize) > 1280 || (srcH / sampleSize) > 720) {
                sampleSize *= 2
            }

            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return Result.failure(IllegalStateException("Failed to decode selected banner image."))

            val maxDim = 960f
            val scale = maxDim / max(decoded.width, decoded.height).toFloat()
            val scaled = if (scale < 1f) {
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                    if (it !== decoded) decoded.recycle()
                }
            } else {
                decoded
            }

            val out = ByteArrayOutputStream()
            var quality = 80
            var bytes: ByteArray
            do {
                out.reset()
                scaled.compress(Bitmap.CompressFormat.JPEG, quality, out)
                bytes = out.toByteArray()
                quality -= 12
            } while (bytes.size > 260_000 && quality >= 32)
            scaled.recycle()

            if (bytes.size > 310_000) {
                return Result.failure(IllegalArgumentException("Banner image is too large. Choose a smaller image."))
            }

            val b64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
            Result.success("data:image/jpeg;base64,$b64")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Converts a stored image string (either an https:// URL or a data:image/...;base64,... URI)
     * into a Coil-compatible image model ([String] URL or decoded [ByteArray]).
     */
    fun toCoilModel(imageData: String): Any {
        if (!imageData.startsWith("data:image/", ignoreCase = true)) {
            return imageData
        }
        val commaIndex = imageData.indexOf(',')
        if (commaIndex == -1 || commaIndex >= imageData.length - 1) {
            return imageData
        }
        return try {
            Base64.decode(imageData.substring(commaIndex + 1), Base64.DEFAULT)
        } catch (_: IllegalArgumentException) {
            imageData
        }
    }
}
