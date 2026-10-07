@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Collections
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel
import com.scanlite.app.data.PageMeta

@Composable
fun DocScreen(vm: ScanViewModel, docId: String) {
    val doc = vm.docs.firstOrNull { it.id == docId } ?: return
    val ctx = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var showExport by remember { mutableStateOf(false) }
    var rename by remember { mutableStateOf(false) }
    var deletePage by remember { mutableStateOf<PageMeta?>(null) }
    val navInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    val pdfSaver = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        if (uri != null) vm.savePdf(doc, uri)
    }
    val dirPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) vm.saveImages(doc, uri)
    }

    Column(Modifier.fillMaxSize()) {
        NavBar(doc.name, onBack = { vm.pop() }) {
            RoundIconButton(Icons.Rounded.Edit, "Rename") { rename = true }
        }
        Box(Modifier.weight(1f).fillMaxWidth()) {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(150.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 120.dp + navInset),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                itemsIndexed(doc.pages, key = { _, p -> p.id }) { i, p ->
                    PageCard(
                        vm, doc.id, p, i, doc.pages.size,
                        onEdit = { vm.editPage(doc.id, p.id) },
                        onMove = { d -> vm.movePage(doc.id, p.id, d) },
                        onDelete = { deletePage = p },
                    )
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(Color.Transparent, cs.background)))
                    .padding(top = 28.dp, bottom = 16.dp + navInset),
                contentAlignment = Alignment.Center,
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    PillButton("Add pages", Icons.Rounded.CameraAlt, filled = false) { vm.startScan(doc.id) }
                    PillButton("Export", Icons.Rounded.Share) { showExport = true }
                }
            }
        }
    }

    if (showExport) {
        ModalBottomSheet(
            onDismissRequest = { showExport = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cs.surface,
        ) {
            Column(Modifier.navigationBarsPadding().padding(bottom = 16.dp)) {
                Text("Export", Modifier.padding(horizontal = 24.dp, vertical = 8.dp), style = MaterialTheme.typography.titleLarge)
                ExportRow(Icons.Rounded.PictureAsPdf, "Save PDF", "Choose where to save it") {
                    showExport = false; pdfSaver.launch(vm.safeName(doc.name) + ".pdf")
                }
                ExportRow(Icons.Rounded.Share, "Share PDF", "Send with any app") {
                    showExport = false; vm.sharePdf(ctx, doc)
                }
                ExportRow(Icons.Rounded.Collections, "Share as images", "JPEG, one per page") {
                    showExport = false; vm.shareImages(ctx, doc)
                }
                ExportRow(Icons.Rounded.FolderOpen, "Save images to folder", "Choose a folder on this phone") {
                    showExport = false; dirPicker.launch(null)
                }
            }
        }
    }
    if (rename) RenameDialog(doc.name, { vm.rename(doc.id, it); rename = false }, { rename = false })
    deletePage?.let { p ->
        ConfirmDialog("Delete page?", "This page will be removed from the scan.", "Delete",
            { vm.deletePage(doc.id, p.id); deletePage = null }, { deletePage = null })
    }
}

@Composable
private fun ExportRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Surface(onClick = onClick, color = Color.Transparent) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(icon, null, tint = cs.primary)
            }
            Spacer(Modifier.width(16.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun PageCard(
    vm: ScanViewModel, docId: String, p: PageMeta, index: Int, total: Int,
    onEdit: () -> Unit, onMove: (Int) -> Unit, onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Box {
        Surface(
            onClick = { menu = true },
            shape = RoundedCornerShape(14.dp),
            color = cs.surface,
            border = BorderStroke(1.dp, cs.outlineVariant),
            modifier = Modifier.aspectRatio(0.74f),
        ) {
            Thumb(vm.store.outFile(docId, p.id), Modifier.fillMaxSize().padding(6.dp))
        }
        Text(
            "${index + 1}",
            Modifier.align(Alignment.BottomCenter).padding(bottom = 8.dp).clip(CircleShape)
                .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 10.dp, vertical = 2.dp),
            color = Color.White, style = MaterialTheme.typography.labelMedium,
        )
        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
            DropdownMenuItem(
                text = { Text("Edit") }, leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                onClick = { menu = false; onEdit() },
            )
            if (index > 0) DropdownMenuItem(
                text = { Text("Move earlier") }, leadingIcon = { Icon(Icons.Rounded.ChevronLeft, null) },
                onClick = { menu = false; onMove(-1) },
            )
            if (index < total - 1) DropdownMenuItem(
                text = { Text("Move later") }, leadingIcon = { Icon(Icons.Rounded.ChevronRight, null) },
                onClick = { menu = false; onMove(1) },
            )
            DropdownMenuItem(
                text = { Text("Delete", color = cs.error) },
                leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = cs.error) },
                onClick = { menu = false; onDelete() },
            )
        }
    }
}
