package com.scanlite.app.imaging

import android.graphics.BitmapFactory
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.OutputStream
import java.util.Locale
import kotlin.math.min

/**
 * Minimal PDF writer that embeds the already-compressed JPEG pages as-is (DCTDecode).
 * Result: tiny PDFs, almost no RAM use (pages are never decoded) - ideal for low-end phones.
 */
object PdfWriter {
    private class Out(val s: OutputStream) {
        var n = 0L
        fun w(t: String) = w(t.toByteArray(Charsets.ISO_8859_1))
        fun w(b: ByteArray) { s.write(b); n += b.size }
        fun copy(f: File) {
            FileInputStream(f).use { ins ->
                val buf = ByteArray(32 * 1024)
                while (true) {
                    val r = ins.read(buf)
                    if (r < 0) break
                    s.write(buf, 0, r)
                    n += r
                }
            }
        }
    }

    private fun f(x: Float) = String.format(Locale.US, "%.2f", x)

    fun write(pages: List<File>, os: OutputStream, a4: Boolean) {
        val out = Out(BufferedOutputStream(os, 64 * 1024))
        val count = pages.size
        val total = 2 + 3 * count
        val offs = LongArray(total + 1)

        out.w("%PDF-1.4\n%\u00e2\u00e3\u00cf\u00d3\n")
        offs[1] = out.n
        out.w("1 0 obj\n<< /Type /Catalog /Pages 2 0 R >>\nendobj\n")
        val kids = (0 until count).joinToString(" ") { "${3 + it * 3} 0 R" }
        offs[2] = out.n
        out.w("2 0 obj\n<< /Type /Pages /Kids [$kids] /Count $count >>\nendobj\n")

        for ((i, file) in pages.withIndex()) {
            val o = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.path, o)
            val iw = o.outWidth
            val ih = o.outHeight

            val pw: Float
            val ph: Float
            val dw: Float
            val dh: Float
            if (a4) {
                val land = iw > ih
                pw = if (land) 841.89f else 595.28f
                ph = if (land) 595.28f else 841.89f
                val k = min(pw / iw, ph / ih)
                dw = iw * k
                dh = ih * k
            } else { // original size at 150 dpi
                pw = iw * 0.48f
                ph = ih * 0.48f
                dw = pw
                dh = ph
            }
            val dx = (pw - dw) / 2f
            val dy = (ph - dh) / 2f

            val po = 3 + i * 3
            val co = po + 1
            val io = po + 2
            val content = "q ${f(dw)} 0 0 ${f(dh)} ${f(dx)} ${f(dy)} cm /Im0 Do Q\n"

            offs[po] = out.n
            out.w("$po 0 obj\n<< /Type /Page /Parent 2 0 R /MediaBox [0 0 ${f(pw)} ${f(ph)}] " +
                "/Resources << /XObject << /Im0 $io 0 R >> >> /Contents $co 0 R >>\nendobj\n")
            offs[co] = out.n
            out.w("$co 0 obj\n<< /Length ${content.length} >>\nstream\n${content}endstream\nendobj\n")
            offs[io] = out.n
            out.w("$io 0 obj\n<< /Type /XObject /Subtype /Image /Width $iw /Height $ih " +
                "/ColorSpace /DeviceRGB /BitsPerComponent 8 /Filter /DCTDecode /Length ${file.length()} >>\nstream\n")
            out.copy(file)
            out.w("\nendstream\nendobj\n")
        }

        val xref = out.n
        out.w("xref\n0 ${total + 1}\n0000000000 65535 f \n")
        for (k in 1..total) out.w(String.format(Locale.US, "%010d 00000 n \n", offs[k]))
        out.w("trailer\n<< /Size ${total + 1} /Root 1 0 R >>\nstartxref\n$xref\n%%EOF\n")
        out.s.flush()
    }
}
