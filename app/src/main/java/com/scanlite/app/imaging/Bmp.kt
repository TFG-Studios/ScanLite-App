package com.scanlite.app.imaging

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

object Bmp {
    private fun sampleFor(w: Int, h: Int, target: Int): Int {
        var s = 1
        while (max(w, h) / (s * 2) >= target) s *= 2
        return s
    }

    /** Memory-safe decode: sub-samples while reading, applies EXIF rotation, caps the long side. */
    fun decode(ctx: Context, uri: Uri, maxDim: Int): Bitmap? {
        return try {
            val cr = ctx.contentResolver
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
            val opts = BitmapFactory.Options().apply {
                inSampleSize = sampleFor(bounds.outWidth, bounds.outHeight, maxDim)
            }
            val src = cr.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
            val orientation = try {
                cr.openInputStream(uri)?.use {
                    ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                } ?: ExifInterface.ORIENTATION_NORMAL
            } catch (e: Exception) {
                ExifInterface.ORIENTATION_NORMAL
            }
            val m = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> m.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> m.postRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> m.postScale(1f, -1f)
                ExifInterface.ORIENTATION_ROTATE_90 -> m.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_270 -> m.postRotate(270f)
            }
            val longSide = max(src.width, src.height)
            if (longSide > maxDim) {
                val s = maxDim / longSide.toFloat()
                m.postScale(s, s)
            }
            if (m.isIdentity) src
            else Bitmap.createBitmap(src, 0, 0, src.width, src.height, m, true).also {
                if (it !== src) src.recycle()
            }
        } catch (e: Throwable) {
            null
        }
    }

    fun decodeFile(f: File, maxDim: Int): Bitmap? = try {
        val b = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(f.path, b)
        if (b.outWidth <= 0) null
        else BitmapFactory.decodeFile(
            f.path,
            BitmapFactory.Options().apply { inSampleSize = sampleFor(b.outWidth, b.outHeight, maxDim) }
        )
    } catch (e: Throwable) {
        null
    }

    fun rotate(b: Bitmap, deg: Int): Bitmap {
        val d = ((deg % 360) + 360) % 360
        if (d == 0) return b
        val m = Matrix().apply { postRotate(d.toFloat()) }
        return Bitmap.createBitmap(b, 0, 0, b.width, b.height, m, true)
    }

    fun saveJpeg(b: Bitmap, f: File, quality: Int) {
        f.parentFile?.mkdirs()
        FileOutputStream(f).use { b.compress(Bitmap.CompressFormat.JPEG, quality, it) }
    }
}
