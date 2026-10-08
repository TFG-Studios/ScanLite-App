package com.scanlite.app.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

data class PageMeta(
    val id: String,
    val corners: List<Float>, // 8 normalized values: TL, TR, BR, BL (x,y)
    val filter: Int,
    val brightness: Int,
    val contrast: Int,
    val sharpness: Int,
    val rotation: Int,
    val strength: Int = 100,
)

data class DocMeta(
    val id: String,
    val name: String,
    val created: Long,
    val pages: List<PageMeta>,
)

/** Plain files + one small JSON per document. No database = smaller app, faster start. */
class Store(ctx: Context) {
    private val root = File(ctx.filesDir, "docs").apply { mkdirs() }

    fun dir(docId: String) = File(root, docId)
    fun outFile(docId: String, pageId: String) = File(dir(docId), "$pageId.jpg")
    fun rawFile(docId: String, pageId: String) = File(dir(docId), "$pageId.raw.jpg")

    fun loadAll(): List<DocMeta> =
        root.listFiles()?.mapNotNull { parse(File(it, "doc.json")) }
            ?.sortedByDescending { it.created } ?: emptyList()

    fun save(doc: DocMeta) {
        val d = dir(doc.id).apply { mkdirs() }
        File(d, "doc.json").writeText(toJson(doc).toString())
    }

    fun delete(docId: String) {
        dir(docId).deleteRecursively()
    }

    fun deletePageFiles(docId: String, pageId: String) {
        outFile(docId, pageId).delete()
        rawFile(docId, pageId).delete()
    }

    private fun toJson(d: DocMeta): JSONObject {
        val pages = JSONArray()
        d.pages.forEach { p ->
            pages.put(
                JSONObject()
                    .put("id", p.id)
                    .put("corners", JSONArray(p.corners.map { it.toDouble() }))
                    .put("filter", p.filter)
                    .put("brightness", p.brightness)
                    .put("contrast", p.contrast)
                    .put("sharpness", p.sharpness)
                    .put("rotation", p.rotation)
                    .put("strength", p.strength)
            )
        }
        return JSONObject().put("id", d.id).put("name", d.name).put("created", d.created).put("pages", pages)
    }

    private fun parse(f: File): DocMeta? = try {
        if (!f.exists()) null else {
            val o = JSONObject(f.readText())
            val arr = o.getJSONArray("pages")
            DocMeta(
                o.getString("id"), o.getString("name"), o.getLong("created"),
                (0 until arr.length()).map { i ->
                    val p = arr.getJSONObject(i)
                    val c = p.getJSONArray("corners")
                    PageMeta(
                        p.getString("id"),
                        (0 until 8).map { c.getDouble(it).toFloat() },
                        p.getInt("filter"), p.getInt("brightness"), p.getInt("contrast"),
                        p.getInt("sharpness"), p.getInt("rotation"), p.optInt("strength", 100),
                    )
                }
            )
        }
    } catch (e: Exception) {
        null
    }
}

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("prefs", Context.MODE_PRIVATE)
    var theme: Int // 0 system, 1 light, 2 dark
        get() = sp.getInt("theme", 0)
        set(v) = sp.edit().putInt("theme", v).apply()
    var quality: Int // 0 fast, 1 balanced, 2 sharp
        get() = sp.getInt("quality", 1)
        set(v) = sp.edit().putInt("quality", v).apply()
    var pdfSize: Int // 0 A4, 1 original
        get() = sp.getInt("pdfSize", 0)
        set(v) = sp.edit().putInt("pdfSize", v).apply()
    var motion: Int // 0 auto, 1 full, 2 reduced
        get() = sp.getInt("motion", 0)
        set(v) = sp.edit().putInt("motion", v).apply()
    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit().putBoolean("haptics", v).apply()
}
