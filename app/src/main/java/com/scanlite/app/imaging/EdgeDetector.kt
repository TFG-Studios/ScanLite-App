package com.scanlite.app.imaging

import android.graphics.Bitmap
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Pure-Kotlin document finder (no OpenCV / ML Kit => tiny APK, works offline).
 * Idea: threshold the downscaled image (Otsu), take the biggest blob that doesn't hug the
 * frame, and use its four extreme points as the page corners.
 */
object EdgeDetector {
    private class Blob(val area: Int, val border: Int, val pts: IntArray)

    private val FALLBACK = listOf(0.06f, 0.06f, 0.94f, 0.06f, 0.94f, 0.94f, 0.06f, 0.94f)

    fun detect(src: Bitmap): List<Float> =
        try { run(src) ?: FALLBACK } catch (e: Throwable) { FALLBACK }

    private fun run(src: Bitmap): List<Float>? {
        val k = 240f / max(src.width, src.height)
        val w = max(24, (src.width * k).roundToInt())
        val h = max(24, (src.height * k).roundToInt())

        // Step-wise halving gives a cleaner downscale than one big bilinear jump.
        var b = src
        while (b.width / 2 >= w && b.height / 2 >= h) {
            val n = Bitmap.createScaledBitmap(b, b.width / 2, b.height / 2, true)
            if (b !== src) b.recycle()
            b = n
        }
        val small = Bitmap.createScaledBitmap(b, w, h, true)
        val px = IntArray(w * h)
        small.getPixels(px, 0, w, 0, 0, w, h)
        if (small !== src) small.recycle()
        if (b !== src && b !== small) b.recycle()

        val gray = IntArray(w * h) {
            val c = px[it]
            (((c shr 16) and 255) * 299 + ((c shr 8) and 255) * 587 + (c and 255) * 114) / 1000
        }
        val g = boxBlur(gray, w, h, 2)
        val t = otsu(g)

        val perimeter = 2f * (w + h)
        val best = listOf(true, false)
            .mapNotNull { largestBlob(g, w, h, t, it) }
            .filter {
                val frac = it.area / (w * h).toFloat()
                frac in 0.12f..0.97f && it.border < 0.30f * perimeter
            }
            .maxByOrNull { it.area } ?: return null

        val out = ArrayList<Float>(8)
        for (i in 0..3) {
            out.add((best.pts[i * 2] + 0.5f) / w)
            out.add((best.pts[i * 2 + 1] + 0.5f) / h)
        }
        // Shoelace area sanity check (normalized coordinates).
        var a = 0f
        for (i in 0..3) {
            val j = (i + 1) % 4
            a += out[i * 2] * out[j * 2 + 1] - out[j * 2] * out[i * 2 + 1]
        }
        return if (abs(a) / 2f < 0.08f) null else out
    }

    private fun boxBlur(a: IntArray, w: Int, h: Int, r: Int): IntArray {
        val t = IntArray(a.size)
        val o = IntArray(a.size)
        val d = 2 * r + 1
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0
            for (k in -r..r) s += a[y * w + (x + k).coerceIn(0, w - 1)]
            t[y * w + x] = s / d
        }
        for (y in 0 until h) for (x in 0 until w) {
            var s = 0
            for (k in -r..r) s += t[(y + k).coerceIn(0, h - 1) * w + x]
            o[y * w + x] = s / d
        }
        return o
    }

    private fun otsu(g: IntArray): Int {
        val hist = IntArray(256)
        for (v in g) hist[v.coerceIn(0, 255)]++
        val total = g.size
        var sum = 0L
        for (i in 0..255) sum += i.toLong() * hist[i]
        var wB = 0
        var sumB = 0L
        var best = 0.0
        var thr = 128
        for (t in 0..255) {
            wB += hist[t]
            if (wB == 0) continue
            val wF = total - wB
            if (wF == 0) break
            sumB += t.toLong() * hist[t]
            val mB = sumB.toDouble() / wB
            val mF = (sum - sumB).toDouble() / wF
            val v = wB.toDouble() * wF * (mB - mF) * (mB - mF)
            if (v > best) { best = v; thr = t }
        }
        return thr
    }

    private fun largestBlob(g: IntArray, w: Int, h: Int, t: Int, bright: Boolean): Blob? {
        val n = w * h
        val seen = BooleanArray(n)
        val stack = IntArray(n)
        var best: Blob? = null
        for (s in 0 until n) {
            if (seen[s] || (g[s] > t) != bright) continue
            var sp = 0
            stack[sp++] = s
            seen[s] = true
            var area = 0
            var border = 0
            val pts = IntArray(8)
            // scores: min(x+y)=TL, max(x-y)=TR, max(x+y)=BR, min(x-y)=BL
            val sc = intArrayOf(Int.MAX_VALUE, Int.MIN_VALUE, Int.MIN_VALUE, Int.MAX_VALUE)
            while (sp > 0) {
                val i = stack[--sp]
                val x = i % w
                val y = i / w
                area++
                if (x == 0 || y == 0 || x == w - 1 || y == h - 1) border++
                val sum = x + y
                val dif = x - y
                if (sum < sc[0]) { sc[0] = sum; pts[0] = x; pts[1] = y }
                if (dif > sc[1]) { sc[1] = dif; pts[2] = x; pts[3] = y }
                if (sum > sc[2]) { sc[2] = sum; pts[4] = x; pts[5] = y }
                if (dif < sc[3]) { sc[3] = dif; pts[6] = x; pts[7] = y }
                if (x > 0) { val j = i - 1; if (!seen[j] && (g[j] > t) == bright) { seen[j] = true; stack[sp++] = j } }
                if (x < w - 1) { val j = i + 1; if (!seen[j] && (g[j] > t) == bright) { seen[j] = true; stack[sp++] = j } }
                if (y > 0) { val j = i - w; if (!seen[j] && (g[j] > t) == bright) { seen[j] = true; stack[sp++] = j } }
                if (y < h - 1) { val j = i + w; if (!seen[j] && (g[j] > t) == bright) { seen[j] = true; stack[sp++] = j } }
            }
            val cur = best
            if (cur == null || area > cur.area) best = Blob(area, border, pts)
        }
        return best
    }
}
