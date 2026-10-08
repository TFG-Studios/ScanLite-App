package com.scanlite.app

import android.app.Application
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.provider.DocumentsContract
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.scanlite.app.data.DocMeta
import com.scanlite.app.data.PageMeta
import com.scanlite.app.data.Prefs
import com.scanlite.app.data.Store
import com.scanlite.app.imaging.Bmp
import com.scanlite.app.imaging.EdgeDetector
import com.scanlite.app.imaging.EditParams
import com.scanlite.app.imaging.PdfWriter
import com.scanlite.app.imaging.Processor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

sealed interface Screen {
    data object Home : Screen
    data object Camera : Screen
    data object Crop : Screen
    data object Filter : Screen
    data class Doc(val id: String) : Screen
    data object Settings : Screen
}

val FULL_FRAME: List<Float> = listOf(0f, 0f, 1f, 0f, 1f, 1f, 0f, 1f)

class ScanViewModel(app: Application) : AndroidViewModel(app) {
    private val ctx: Context = app.applicationContext
    val store = Store(ctx)
    private val prefs = Prefs(ctx)

    var docs by mutableStateOf<List<DocMeta>>(emptyList())
        private set
    var stack by mutableStateOf<List<Screen>>(listOf(Screen.Home))
        private set
    var busy by mutableStateOf(false)
        private set
    var message by mutableStateOf<String?>(null)

    var theme by mutableIntStateOf(prefs.theme)
        private set
    var quality by mutableIntStateOf(prefs.quality)
        private set
    var pdfSize by mutableIntStateOf(prefs.pdfSize)
        private set
    var motion by mutableIntStateOf(prefs.motion)
        private set
    var haptics by mutableStateOf(prefs.haptics)
        private set

    // ---- page currently being scanned / edited ----
    var raw by mutableStateOf<Bitmap?>(null)
        private set
    var corners by mutableStateOf(FULL_FRAME)
    var base by mutableStateOf<Bitmap?>(null)
        private set
    var params by mutableStateOf(EditParams())
    var queueLeft by mutableIntStateOf(0)
        private set
    private var editing by mutableStateOf<PageMeta?>(null)

    val isEditing: Boolean get() = editing != null
    val canAddMore: Boolean get() = queueLeft > 0 || Screen.Camera in stack

    private var sessionDocId: String? = null
    private val queue = ArrayDeque<Uri>()

    private val maxDim get() = when (quality) { 0 -> 1400; 2 -> 2400; else -> 1800 }
    private val rawMax get() = if (quality == 2) 2600 else 2200
    private val jpegQ get() = when (quality) { 0 -> 78; 2 -> 88; else -> 84 }

    init {
        File(ctx.cacheDir, "capture").deleteRecursively()
        viewModelScope.launch { docs = withContext(Dispatchers.IO) { store.loadAll() } }
    }

    // ---------------- navigation ----------------
    fun push(s: Screen) { stack = stack + s }
    fun pop() { if (stack.size > 1) stack = stack.dropLast(1) }

    fun startScan(docId: String?) {
        sessionDocId = docId
        push(Screen.Camera)
    }

    fun importFrom(docId: String?, uris: List<Uri>) {
        sessionDocId = docId
        onImages(uris)
    }

    // ---------------- capture / import ----------------
    fun onImages(uris: List<Uri>) {
        queue.addAll(uris)
        queueLeft = queue.size
        val top = stack.last()
        if (!busy && top != Screen.Crop && top != Screen.Filter) loadNext()
    }

    private fun loadNext() {
        val uri = queue.removeFirstOrNull() ?: return
        queueLeft = queue.size
        viewModelScope.launch {
            busy = true
            val result = withContext(Dispatchers.Default) {
                Bmp.decode(ctx, uri, rawMax)?.let { it to EdgeDetector.detect(it) }
            }
            if (uri.scheme == "file") uri.path?.let { File(it).delete() }
            busy = false
            if (result == null) {
                message = "Couldn't open that image"
                if (queue.isNotEmpty()) loadNext()
                return@launch
            }
            raw = result.first
            corners = result.second
            params = EditParams()
            editing = null
            push(Screen.Crop)
        }
    }

    fun editPage(docId: String, pageId: String) {
        val doc = docs.firstOrNull { it.id == docId } ?: return
        val pm = doc.pages.firstOrNull { it.id == pageId } ?: return
        viewModelScope.launch {
            busy = true
            val bmp = withContext(Dispatchers.IO) { Bmp.decodeFile(store.rawFile(docId, pageId), 4000) }
            busy = false
            if (bmp == null) { message = "Original image not found"; return@launch }
            sessionDocId = docId
            raw = bmp
            corners = pm.corners
            params = EditParams(pm.filter, pm.brightness, pm.contrast, pm.sharpness, pm.rotation, pm.strength)
            editing = pm
            push(Screen.Crop)
        }
    }

    // ---------------- crop ----------------
    fun autoDetect() {
        val r = raw ?: return
        viewModelScope.launch { corners = withContext(Dispatchers.Default) { EdgeDetector.detect(r) } }
    }

    fun selectAll() { corners = FULL_FRAME }

    fun confirmCrop() {
        val r = raw ?: return
        val c = corners
        viewModelScope.launch {
            busy = true
            base = withContext(Dispatchers.Default) { Processor.warp(r, c, 1000) }
            busy = false
            push(Screen.Filter)
        }
    }

    // ---------------- save page ----------------
    fun savePage(done: Boolean) {
        val r = raw ?: return
        val p = params
        val c = corners
        val old = editing
        viewModelScope.launch {
            busy = true
            val updated: DocMeta? = try {
                val doc = sessionDocId?.let { id -> docs.firstOrNull { it.id == id } }
                    ?: DocMeta(newId(), defaultName(), System.currentTimeMillis(), emptyList())
                val pageId = old?.id ?: newId()
                withContext(Dispatchers.Default) {
                    val warped = Processor.warp(r, c, maxDim)
                    val out = Processor.process(warped, p)
                    store.dir(doc.id).mkdirs()
                    Bmp.saveJpeg(out, store.outFile(doc.id, pageId), jpegQ)
                    if (old == null) Bmp.saveJpeg(r, store.rawFile(doc.id, pageId), 88)
                    warped.recycle()
                    out.recycle()
                }
                val meta = PageMeta(pageId, c, p.filter, p.brightness, p.contrast, p.sharpness, p.rotation, p.strength)
                val newDoc = doc.copy(
                    pages = if (old != null) doc.pages.map { if (it.id == pageId) meta else it }
                    else doc.pages + meta
                )
                withContext(Dispatchers.IO) { store.save(newDoc) }
                newDoc
            } catch (e: Throwable) {
                null
            }
            busy = false
            if (updated == null) { message = "Couldn't save the page"; return@launch }

            sessionDocId = updated.id
            docs = (listOf(updated) + docs.filter { it.id != updated.id }).sortedByDescending { it.created }

            if (done || old != null) {
                queue.clear(); queueLeft = 0
                clearWork()
                stack = listOf(Screen.Home, Screen.Doc(updated.id))
            } else {
                stack = stack.dropLast(2) // back to camera (or home)
                clearWork()
                if (queue.isNotEmpty()) loadNext()
            }
        }
    }

    private fun clearWork() { raw = null; base = null; editing = null }

    // ---------------- documents ----------------
    private fun update(id: String, f: (DocMeta) -> DocMeta) {
        val cur = docs.firstOrNull { it.id == id } ?: return
        val n = f(cur)
        docs = docs.map { if (it.id == id) n else it }
        viewModelScope.launch(Dispatchers.IO) { store.save(n) }
    }

    fun rename(id: String, name: String) = update(id) { it.copy(name = name.trim().ifBlank { it.name }) }

    fun deleteDoc(id: String) {
        docs = docs.filter { it.id != id }
        if (stack.any { it is Screen.Doc && it.id == id }) stack = listOf(Screen.Home)
        viewModelScope.launch(Dispatchers.IO) { store.delete(id) }
    }

    fun deletePage(docId: String, pageId: String) {
        val d = docs.firstOrNull { it.id == docId } ?: return
        if (d.pages.size <= 1) { deleteDoc(docId); return }
        update(docId) { it.copy(pages = it.pages.filter { p -> p.id != pageId }) }
        viewModelScope.launch(Dispatchers.IO) { store.deletePageFiles(docId, pageId) }
    }

    fun movePage(docId: String, pageId: String, delta: Int) = update(docId) { d ->
        val list = d.pages.toMutableList()
        val i = list.indexOfFirst { it.id == pageId }
        val j = i + delta
        if (i < 0 || j !in list.indices) d
        else { val t = list[i]; list[i] = list[j]; list[j] = t; d.copy(pages = list) }
    }

    // ---------------- export ----------------
    fun safeName(n: String) = n.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifEmpty { "Scan" }

    private fun files(doc: DocMeta) = doc.pages.map { store.outFile(doc.id, it.id) }

    private fun runIO(ok: String, block: () -> Unit) {
        viewModelScope.launch {
            busy = true
            val err = withContext(Dispatchers.IO) { try { block(); null } catch (t: Throwable) { t } }
            busy = false
            message = if (err == null) ok else "Failed: ${err.message ?: "unknown error"}"
        }
    }

    fun savePdf(doc: DocMeta, uri: Uri) = runIO("PDF saved") {
        ctx.contentResolver.openOutputStream(uri)?.use { PdfWriter.write(files(doc), it, pdfSize == 0) }
            ?: error("Can't write to that location")
    }

    fun saveImages(doc: DocMeta, tree: Uri) = runIO("Images saved") {
        val parent = DocumentsContract.buildDocumentUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree))
        doc.pages.forEachIndexed { i, p ->
            val dst = DocumentsContract.createDocument(
                ctx.contentResolver, parent, "image/jpeg", "${safeName(doc.name)}_${i + 1}.jpg"
            ) ?: error("Can't create file")
            ctx.contentResolver.openOutputStream(dst)?.use { o ->
                store.outFile(doc.id, p.id).inputStream().use { it.copyTo(o) }
            }
        }
    }

    fun sharePdf(activity: Context, doc: DocMeta) {
        viewModelScope.launch {
            busy = true
            val file = withContext(Dispatchers.IO) {
                try {
                    val f = File(shareDir(), safeName(doc.name) + ".pdf")
                    FileOutputStream(f).use { PdfWriter.write(files(doc), it, pdfSize == 0) }
                    f
                } catch (t: Throwable) { null }
            }
            busy = false
            if (file == null) message = "Couldn't create the PDF" else send(activity, listOf(file), "application/pdf")
        }
    }

    fun shareImages(activity: Context, doc: DocMeta) {
        viewModelScope.launch {
            busy = true
            val list = withContext(Dispatchers.IO) {
                try {
                    val dir = shareDir()
                    doc.pages.mapIndexed { i, p ->
                        File(dir, "${safeName(doc.name)}_${i + 1}.jpg").also { store.outFile(doc.id, p.id).copyTo(it, true) }
                    }
                } catch (t: Throwable) { null }
            }
            busy = false
            if (list == null) message = "Couldn't prepare the images" else send(activity, list, "image/jpeg")
        }
    }

    private fun shareDir() = File(ctx.cacheDir, "shared").apply { deleteRecursively(); mkdirs() }

    private fun send(c: Context, files: List<File>, mime: String) {
        val uris = arrayListOf<Uri>().apply {
            files.forEach { add(FileProvider.getUriForFile(ctx, "${ctx.packageName}.fileprovider", it)) }
        }
        val i = Intent(if (uris.size == 1) Intent.ACTION_SEND else Intent.ACTION_SEND_MULTIPLE).apply {
            type = mime
            if (uris.size == 1) putExtra(Intent.EXTRA_STREAM, uris[0])
            else putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        c.startActivity(Intent.createChooser(i, null))
    }

    // ---------------- settings ----------------
    fun changeTheme(v: Int) { theme = v; prefs.theme = v }
    fun changeQuality(v: Int) { quality = v; prefs.quality = v }
    fun changePdfSize(v: Int) { pdfSize = v; prefs.pdfSize = v }
    fun changeMotion(v: Int) { motion = v; prefs.motion = v }
    fun changeHaptics(v: Boolean) { haptics = v; prefs.haptics = v }

    private fun newId() = UUID.randomUUID().toString().take(12)
    private fun defaultName() = "Scan " + SimpleDateFormat("d MMM, HH:mm", Locale.getDefault()).format(Date())
}
