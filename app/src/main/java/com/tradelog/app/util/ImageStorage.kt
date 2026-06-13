package com.tradelog.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

/**
 * Copies a picked image into app-private storage, downscaled and JPEG-compressed.
 * Keeps stored screenshots small so lists stay smooth on low-end devices.
 */
object ImageStorage {
    private const val MAX_DIM = 1280
    private const val QUALITY = 80

    suspend fun importImage(context: Context, source: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "screenshots").apply { mkdirs() }

            // Decode bounds first to compute a sample size.
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, bounds)
            }
            val sample = computeSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIM)

            val opts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = context.contentResolver.openInputStream(source)?.use {
                BitmapFactory.decodeStream(it, null, opts)
            } ?: return@runCatching null

            val out = File(dir, "shot_${System.currentTimeMillis()}.jpg")
            FileOutputStream(out).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, fos)
            }
            bitmap.recycle()
            out.absolutePath
        }.getOrNull()
    }

    fun delete(path: String?) {
        if (path == null) return
        runCatching { File(path).takeIf { it.exists() }?.delete() }
    }

    private fun computeSampleSize(width: Int, height: Int, maxDim: Int): Int {
        if (width <= 0 || height <= 0) return 1
        var sample = 1
        var w = width
        var h = height
        while (w / 2 >= maxDim || h / 2 >= maxDim) {
            w /= 2; h /= 2; sample *= 2
        }
        return sample
    }
}
