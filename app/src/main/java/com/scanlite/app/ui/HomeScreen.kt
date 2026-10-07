@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.scanlite.app.Screen
import com.scanlite.app.ScanViewModel
import com.scanlite.app.data.DocMeta

@Composable
fun HomeScreen(vm: ScanViewModel) {
    val cs = MaterialTheme.colorScheme
    var query by remember { mutableStateOf("") }
    var renameTarget by remember { mutableStateOf<DocMeta?>(null) }
    var deleteTarget by remember { mutableStateOf<DocMeta?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.importFrom(null, uris)
    }
    val shown = vm.docs.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 120.dp + bottomInset),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.statusBarsPadding().padding(top = 8.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        RoundIconButton(Icons.Rounded.Settings, "Settings") { vm.push(Screen.Settings) }
                    }
                    Text(
                        "Scans",
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
                    )
                    if (vm.docs.isNotEmpty()) {
                        TextField(
                            value = query,
                            onValueChange = { query = it },
                            singleLine = true,
                            placeholder = { Text("Search") },
                            leadingIcon = { Icon(Icons.Rounded.Search, null) },
                            shape = RoundedCornerShape(12.dp),
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = cs.surfaceVariant,
                                unfocusedContainerColor = cs.surfaceVariant,
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
            if (vm.docs.isEmpty()) {
                item { EmptyState() }
            } else {
                items(shown, key = { it.id }) { d ->
                    DocRow(
                        vm, d,
                        onOpen = { vm.push(Screen.Doc(d.id)) },
                        onRename = { renameTarget = d },
                        onDelete = { deleteTarget = d },
                    )
                }
            }
        }

        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, cs.background)))
                .padding(top = 28.dp, bottom = 16.dp + bottomInset),
            contentAlignment = Alignment.Center,
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton("Import", Icons.Rounded.PhotoLibrary, filled = false) { picker.launch("image/*") }
                PillButton("Scan", Icons.Rounded.CameraAlt) { vm.startScan(null) }
            }
        }
    }

    renameTarget?.let { d ->
        RenameDialog(d.name, { vm.rename(d.id, it); renameTarget = null }, { renameTarget = null })
    }
    deleteTarget?.let { d ->
        ConfirmDialog("Delete scan?", "\"${d.name}\" and its pages will be removed from this phone.", "Delete",
            { vm.deleteDoc(d.id); deleteTarget = null }, { deleteTarget = null })
    }
}

@Composable
private fun EmptyState() {
    val cs = MaterialTheme.colorScheme
    Column(
        Modifier.fillMaxWidth().padding(top = 96.dp, start = 24.dp, end = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(96.dp).clip(CircleShape).background(cs.primaryContainer), contentAlignment = Alignment.Center) {
            Icon(Icons.Rounded.Description, null, Modifier.size(44.dp), tint = cs.primary)
        }
        Spacer(Modifier.height(20.dp))
        Text("No scans yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Scan a page, ID card or whiteboard.\nEverything stays on this phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DocRow(vm: ScanViewModel, d: DocMeta, onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    Surface(onClick = onOpen, shape = RoundedCornerShape(18.dp), color = cs.surface) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(width = 56.dp, height = 72.dp).clip(RoundedCornerShape(8.dp)).background(cs.surfaceVariant)) {
                d.pages.firstOrNull()?.let {
                    Thumb(vm.store.outFile(d.id, it.id), Modifier.fillMaxSize(), ContentScale.Crop, px = 240)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Spacer(Modifier.height(2.dp))
                val n = d.pages.size
                Text(
                    "$n ${if (n == 1) "page" else "pages"} · ${fmtDate(d.created)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = cs.onSurfaceVariant,
                )
            }
            Box {
                IconButton({ menu = true }) { Icon(Icons.Rounded.MoreVert, "More", tint = cs.onSurfaceVariant) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        leadingIcon = { Icon(Icons.Rounded.Edit, null) },
                        onClick = { menu = false; onRename() },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = cs.error) },
                        leadingIcon = { Icon(Icons.Rounded.Delete, null, tint = cs.error) },
                        onClick = { menu = false; onDelete() },
                    )
                }
            }
        }
    }
}
