package com.scanlite.app.imaging

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.Paint
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

object Filters {
    const val ORIGINAL = 0
    const val ENHANCE = 1
    const val DOCUMENT = 2
    const val GRAY = 3
    const val WHITEBOARD = 4

    val names = listOf("Original", "Enhance", "Document", "Gray", "Board")

    fun defaults(f: Int) = EditParams(
        filter = f,
        sharpness = when (f) {
            ORIGINAL -> 0
            DOCUMENT -> 35
            WHITEBOARD -> 20
            else -> 25
        }
    )
}

data class EditParams(
    val filter: Int = Filters.ENHANCE,
    val brightness: Int = 0,   // -100..100
    val contrast: Int = 0,     // -100..100
    val sharpness: Int = 25,   // 0..100
    val rotation: Int = 0,     // 0, 90, 180, 270
    val strength: Int = 100,   // filter intensity 0..100
)

object Processor {

    /** Perspective-correct the quad (normalized TL,TR,BR,BL) into a flat rectangle. */
    fun warp(src: Bitmap, c: List<Float>, maxDim: Int): Bitmap {
        val sw = src.width.toFloat()
        val sh = src.height.toFloat()
        val p = floatArrayOf(
            c[0] * sw, c[1] * sh, c[2] * sw, c[3] * sh,
            c[4] * sw, c[5] * sh, c[6] * sw, c[7] * sh,
        )
        fun d(a: Int, b: Int) = hypot(p[a * 2] - p[b * 2], p[a * 2 + 1] - p[b * 2 + 1])
        var w = max(d(0, 1), d(3, 2))
        var h = max(d(0, 3), d(1, 2))
        val k = min(1f, maxDim / max(max(w, h), 1f))
        w *= k
        h *= k
        val ow = max(32, w.roundToInt())
        val oh = max(32, h.roundToInt())
        val out = Bitmap.createBitmap(ow, oh, Bitmap.Config.ARGB_8888)
        val m = Matrix()
        m.setPolyToPoly(
            p, 0,
            floatArrayOf(0f, 0f, ow.toFloat(), 0f, ow.toFloat(), oh.toFloat(), 0f, oh.toFloat()), 0, 4
        )
        val canvas = Canvas(out)
        canvas.drawColor(Color.WHITE)
        canvas.drawBitmap(src, m, Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG))
        return out
    }

    fun shrink(b: Bitmap, maxDim: Int): Bitmap {
        val k = maxDim / max(b.width, b.height).toFloat()
        if (k >= 1f) return b
        return Bitmap.createScaledBitmap(
            b, max(1, (b.width * k).roundToInt()), max(1, (b.height * k).roundToInt()), true
        )
    }

    /** Never modifies [src]. Returns a new bitmap. */
    fun process(src: Bitmap, p: EditParams): Bitmap {
        val s = Bmp.rotate(src, p.rotation)
        val w = s.width
        val h = s.height
        val px = IntArray(w * h)
        s.getPixels(px, 0, w, 0, 0, w, h)
        if (s !== src) s.recycle()

        val orig: IntArray? = if (p.filter != Filters.ORIGINAL && p.strength < 100) px.copyOf() else null
        val mono = p.filter == Filters.DOCUMENT || p.filter == Filters.GRAY
        if (mono) {
            for (i in px.indices) {
                val c = px[i]
                val l = (((c shr 16) and 255) * 299 + ((c shr 8) and 255) * 587 + (c and 255) * 114) / 1000
                px[i] = (0xFF shl 24) or (l shl 16) or (l shl 8) or l
            }
        }
        when (p.filter) {
            Filters.ENHANCE -> normalize(px, w, h, 110, 0.85f, false)
            Filters.DOCUMENT -> normalize(px, w, h, 80, 1f, false)
            Filters.GRAY -> normalize(px, w, h, 100, 0.9f, false)
            Filters.WHITEBOARD -> normalize(px, w, h, 90, 1f, true)
        }
        applyTone(px, p)
        if (orig != null) {
            val t = p.strength / 100f
            for (i in px.indices) {
                val a = orig[i]
                val b = px[i]
                val r = (((a shr 16) and 255) + ((((b shr 16) and 255) - ((a shr 16) and 255)) * t)).toInt()
                val g = (((a shr 8) and 255) + ((((b shr 8) and 255) - ((a shr 8) and 255)) * t)).toInt()
                val bl = ((a and 255) + (((b and 255) - (a and 255)) * t)).toInt()
                px[i] = (0xFF shl 24) or (r.coerceIn(0, 255) shl 16) or (g.coerceIn(0, 255) shl 8) or bl.coerceIn(0, 255)
            }
        }
        if (p.sharpness > 0) sharpen(px, w, h, p.sharpness / 100f * 0.8f)
        return Bitmap.createBitmap(px, w, h, Bitmap.Config.ARGB_8888)
    }

    /**
     * Removes shadows / uneven light: estimate the paper colour on a coarse grid
     * (cell average -> grow to ignore ink -> smooth), then divide the image by it.
     */
    private fun normalize(px: IntArray, w: Int, h: Int, floor: Int, strength: Float, perChannel: Boolean) {
        val cell = max(8, max(w, h) / 90)
        val gw = (w + cell - 1) / cell
        val gh = (h + cell - 1) / cell
        val n = gw * gh
        val sr = FloatArray(n)
        val sg = FloatArray(n)
        val sb = FloatArray(n)
        val cnt = IntArray(n)
        for (y in 0 until h) {
            val gy = (y / cell) * gw
            val row = y * w
            for (x in 0 until w) {
                val c = px[row + x]
                val i = gy + x / cell
                sr[i] += ((c shr 16) and 255).toFloat()
                sg[i] += ((c shr 8) and 255).toFloat()
                sb[i] += (c and 255).toFloat()
                cnt[i]++
            }
        }
        for (i in 0 until n) { sr[i] /= cnt[i]; sg[i] /= cnt[i]; sb[i] /= cnt[i] }
        if (!perChannel) {
            for (i in 0 until n) {
                val l = 0.299f * sr[i] + 0.587f * sg[i] + 0.114f * sb[i]
                sr[i] = l; sg[i] = l; sb[i] = l
            }
        }
        val br = smooth(sr, gw, gh)
        val bg = smooth(sg, gw, gh)
        val bb = smooth(sb, gw, gh)

        val x0 = IntArray(w)
        val x1 = IntArray(w)
        val xf = FloatArray(w)
        for (x in 0 until w) {
            val f = ((x + 0.5f) / cell - 0.5f).coerceIn(0f, gw - 1f)
            val i = f.toInt()
            x0[x] = i; x1[x] = min(i + 1, gw - 1); xf[x] = f - i
        }
        val fl = floor.toFloat()
        for (y in 0 until h) {
            val f = ((y + 0.5f) / cell - 0.5f).coerceIn(0f, gh - 1f)
            val y0 = f.toInt()
            val y1 = min(y0 + 1, gh - 1)
            val yf = f - y0
            val r0 = y0 * gw
            val r1 = y1 * gw
            val row = y * w
            for (x in 0 until w) {
                val a = xf[x]
                val i00 = r0 + x0[x]; val i01 = r0 + x1[x]
                val i10 = r1 + x0[x]; val i11 = r1 + x1[x]
                val c = px[row + x]
                val gr = gain(bil(br, i00, i01, i10, i11, a, yf), fl, strength)
                val gg = gain(bil(bg, i00, i01, i10, i11, a, yf), fl, strength)
                val gb = gain(bil(bb, i00, i01, i10, i11, a, yf), fl, strength)
                val r = (((c shr 16) and 255) * gr).toInt().coerceAtMost(255)
                val g = (((c shr 8) and 255) * gg).toInt().coerceAtMost(255)
                val b = ((c and 255) * gb).toInt().coerceAtMost(255)
                px[row + x] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }

    private fun gain(bg: Float, floor: Float, strength: Float): Float =
        255f / (255f - strength * (255f - max(bg, floor)))

    private fun bil(a: FloatArray, i00: Int, i01: Int, i10: Int, i11: Int, fx: Float, fy: Float): Float {
        val t = a[i00] + (a[i01] - a[i00]) * fx
        val b = a[i10] + (a[i11] - a[i10]) * fx
        return t + (b - t) * fy
    }

    private fun smooth(a: FloatArray, gw: Int, gh: Int): FloatArray =
        pass(pass(a, gw, gh, 3, true), gw, gh, 2, false)

    private fun pass(a: FloatArray, gw: Int, gh: Int, r: Int, useMax: Boolean): FloatArray {
        val t = FloatArray(a.size)
        val o = FloatArray(a.size)
        for (y in 0 until gh) for (x in 0 until gw) {
            var acc = 0f
            for (k in -r..r) {
                val v = a[y * gw + (x + k).coerceIn(0, gw - 1)]
                if (useMax) { if (v > acc) acc = v } else acc += v
            }
            t[y * gw + x] = if (useMax) acc else acc / (2 * r + 1)
        }
        for (y in 0 until gh) for (x in 0 until gw) {
            var acc = 0f
            for (k in -r..r) {
                val v = t[(y + k).coerceIn(0, gh - 1) * gw + x]
                if (useMax) { if (v > acc) acc = v } else acc += v
            }
            o[y * gw + x] = if (useMax) acc else acc / (2 * r + 1)
        }
        return o
    }

    private fun applyTone(px: IntArray, p: EditParams) {
        val cf = 1f + p.contrast / 100f
        val bo = p.brightness / 200f
        val (lo, hi) = when (p.filter) {
            Filters.DOCUMENT -> 0.40f to 0.88f
            Filters.WHITEBOARD -> 0.20f to 0.86f
            Filters.ENHANCE -> 0.03f to 0.95f
            Filters.GRAY -> 0.04f to 0.94f
            else -> 0f to 1f
        }
        val lut = IntArray(256) { i ->
            var v = ((i / 255f - lo) / (hi - lo)).coerceIn(0f, 1f)
            v = ((v - 0.5f) * cf + 0.5f + bo).coerceIn(0f, 1f)
            (v * 255f + 0.5f).toInt()
        }
        val sat = when (p.filter) {
            Filters.ENHANCE -> 1.08f
            Filters.WHITEBOARD -> 1.3f
            else -> 1f
        }
        for (i in px.indices) {
            val c = px[i]
            var r = lut[(c shr 16) and 255]
            var g = lut[(c shr 8) and 255]
            var b = lut[c and 255]
            if (sat != 1f) {
                val l = 0.299f * r + 0.587f * g + 0.114f * b
                r = (l + (r - l) * sat).toInt().coerceIn(0, 255)
                g = (l + (g - l) * sat).toInt().coerceIn(0, 255)
                b = (l + (b - l) * sat).toInt().coerceIn(0, 255)
            }
            px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
    }

    private fun sharpen(px: IntArray, w: Int, h: Int, a: Float) {
        if (w < 3 || h < 3) return
        val o = px.copyOf()
        val k = 1f + 4f * a
        for (y in 1 until h - 1) {
            for (x in 1 until w - 1) {
                val i = y * w + x
                val c = o[i]; val n = o[i - w]; val s = o[i + w]; val e = o[i + 1]; val ww = o[i - 1]
                val r = (k * ((c shr 16) and 255) - a * (((n shr 16) and 255) + ((s shr 16) and 255) + ((e shr 16) and 255) + ((ww shr 16) and 255))).toInt().coerceIn(0, 255)
                val g = (k * ((c shr 8) and 255) - a * (((n shr 8) and 255) + ((s shr 8) and 255) + ((e shr 8) and 255) + ((ww shr 8) and 255))).toInt().coerceIn(0, 255)
                val b = (k * (c and 255) - a * ((n and 255) + (s and 255) + (e and 255) + (ww and 255))).toInt().coerceIn(0, 255)
                px[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
    }
}
