@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.PhotoLibrary
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
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
    var grid by rememberSaveable { mutableStateOf(true) }
    var renameTarget by remember { mutableStateOf<DocMeta?>(null) }
    var deleteTarget by remember { mutableStateOf<DocMeta?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.importFrom(null, uris)
    }
    val shown = vm.docs.filter { it.name.contains(query.trim(), ignoreCase = true) }
    val gridState = rememberLazyGridState()
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val collapsed by remember {
        derivedStateOf { gridState.firstVisibleItemIndex > 0 || gridState.firstVisibleItemScrollOffset > 90 }
    }
    val barAlpha by animateFloatAsState(if (collapsed) 1f else 0f, tween(180), label = "bar")

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Fixed(if (grid) 2 else 1),
            state = gridState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = 16.dp, end = 16.dp, top = topInset + 56.dp, bottom = 150.dp + bottomInset,
            ),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(if (grid) 20.dp else 10.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 4.dp, bottom = 8.dp)) {
                    Text("Scans", style = MaterialTheme.typography.headlineLarge)
                    Text(
                        if (vm.docs.isEmpty()) "Everything stays on this phone"
                        else "${vm.docs.size} ${if (vm.docs.size == 1) "document" else "documents"} · private & offline",
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                    if (vm.docs.isNotEmpty()) {
                        Spacer(Modifier.height(14.dp))
                        SearchPill(query, { query = it })
                    }
                }
            }
            if (vm.docs.isEmpty()) {
                item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { EmptyState() }
            } else if (shown.isEmpty()) {
                item(key = "none", span = { GridItemSpan(maxLineSpan) }) {
                    Text(
                        "No scans match “$query”",
                        Modifier.fillMaxWidth().padding(top = 48.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium,
                        color = cs.onSurfaceVariant,
                    )
                }
            } else {
                itemsIndexed(shown, key = { _, d -> d.id }) { i, d ->
                    val open = { vm.push(Screen.Doc(d.id)) }
                    val rename = { renameTarget = vm.docs.firstOrNull { it.id == d.id } }
                    val delete = { deleteTarget = vm.docs.firstOrNull { it.id == d.id } }
                    if (grid) DocCard(vm, d, i, open, rename, delete)
                    else DocRow(vm, d, i, open, rename, delete)
                }
            }
        }

        // Top bar: transparent over the large title, solid once collapsed.
        Box(Modifier.fillMaxWidth().background(cs.background.copy(alpha = 0.95f * barAlpha))) {
            Row(
                Modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    "Scans",
                    Modifier.weight(1f).graphicsLayer { alpha = barAlpha },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (vm.docs.isNotEmpty()) {
                    RoundIconButton(
                        if (grid) Icons.Outlined.ViewAgenda else Icons.Outlined.GridView,
                        if (grid) "List view" else "Grid view",
                    ) { grid = !grid }
                }
            }
            Box(
                Modifier.align(Alignment.BottomCenter).fillMaxWidth().height(0.5.dp)
                    .background(cs.outlineVariant.copy(alpha = barAlpha))
            )
        }

        // Floating dock
        Box(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth()
                .background(Brush.verticalGradient(listOf(Color.Transparent, cs.background)))
                .padding(top = 36.dp, bottom = 14.dp + bottomInset),
            contentAlignment = Alignment.Center,
        ) {
            val dockShape = RoundedCornerShape(36.dp)
            Row(
                Modifier.shadow(14.dp, dockShape)
                    .background(cs.surface, dockShape)
                    .border(0.5.dp, cs.outlineVariant, dockShape)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                IconLabelButton(Icons.Outlined.PhotoLibrary, "Import") { picker.launch("image/*") }
                ScanButton { vm.startScan(null) }
                IconLabelButton(Icons.Outlined.Settings, "Settings") { vm.push(Screen.Settings) }
            }
        }
    }

    renameTarget?.let { d ->
        RenameDialog(d.name, { vm.rename(d.id, it); renameTarget = null }, { renameTarget = null })
    }
    deleteTarget?.let { d ->
        ConfirmDialog(
            "Delete scan?", "“${d.name}” and its pages will be removed from this phone.", "Delete",
            { vm.deleteDoc(d.id); deleteTarget = null }, { deleteTarget = null },
        )
    }
}

@Composable
private fun ScanButton(onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Box(
        Modifier.size(66.dp).bounceClick(pressedScale = 0.9f, onClick = onClick)
            .shadow(12.dp, CircleShape, ambientColor = cs.primary, spotColor = cs.primary)
            .background(
                Brush.verticalGradient(listOf(lerp(cs.primary, Color.White, 0.22f), cs.primary)), CircleShape
            ),
        contentAlignment = Alignment.Center,
    ) { Icon(Icons.Outlined.CameraAlt, "Scan", Modifier.size(30.dp), tint = cs.onPrimary) }
}

@Composable
private fun EmptyState() {
    val cs = MaterialTheme.colorScheme
    val prog = rememberLoop(LocalReduceMotion.current, 1800)
    Column(Modifier.fillMaxWidth().padding(top = 56.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Canvas(Modifier.size(150.dp, 190.dp)) {
            val w = size.width
            val h = size.height
            val paperTL = Offset(w * 0.12f, h * 0.08f)
            val paperSize = Size(w * 0.76f, h * 0.84f)
            drawRoundRect(cs.surface, paperTL, paperSize, CornerRadius(16.dp.toPx()))
            drawRoundRect(cs.outlineVariant, paperTL, paperSize, CornerRadius(16.dp.toPx()), style = Stroke(1.dp.toPx()))
            for (k in 0..4) {
                drawRoundRect(
                    cs.surfaceVariant,
                    Offset(w * 0.22f, h * (0.2f + k * 0.1f)),
                    Size(w * (if (k == 4) 0.3f else 0.56f), 6.dp.toPx()),
                    CornerRadius(3.dp.toPx()),
                )
            }
            val len = 20.dp.toPx()
            val sw = 3.5.dp.toPx()
            val l = w * 0.04f; val t = h * 0.03f; val r = w * 0.96f; val b = h * 0.97f
            fun corner(x: Float, y: Float, dx: Float, dy: Float) {
                drawLine(cs.primary, Offset(x, y), Offset(x + dx * len, y), sw, StrokeCap.Round)
                drawLine(cs.primary, Offset(x, y), Offset(x, y + dy * len), sw, StrokeCap.Round)
            }
            corner(l, t, 1f, 1f); corner(r, t, -1f, 1f); corner(l, b, 1f, -1f); corner(r, b, -1f, -1f)
            val y = h * 0.12f + prog * h * 0.76f
            drawLine(cs.primary.copy(alpha = 0.9f), Offset(w * 0.1f, y), Offset(w * 0.9f, y), 3.dp.toPx(), StrokeCap.Round)
        }
        Spacer(Modifier.height(22.dp))
        Text("No scans yet", style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(6.dp))
        Text(
            "Tap the camera to scan a page, ID card\nor whiteboard.",
            style = MaterialTheme.typography.bodyMedium,
            color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun DocMenu(expanded: Boolean, onDismiss: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        containerColor = cs.surface,
        shadowElevation = 10.dp,
        tonalElevation = 0.dp,
    ) {
        DropdownMenuItem(
            text = { Text("Rename", style = MaterialTheme.typography.bodyLarge) },
            leadingIcon = { Icon(Icons.Outlined.Edit, null) },
            onClick = { onDismiss(); onRename() },
        )
        DropdownMenuItem(
            text = { Text("Delete", style = MaterialTheme.typography.bodyLarge, color = cs.error) },
            leadingIcon = { Icon(Icons.Outlined.Delete, null, tint = cs.error) },
            onClick = { onDismiss(); onDelete() },
        )
    }
}

@Composable
private fun androidx.compose.foundation.lazy.grid.LazyGridItemScope.DocCard(
    vm: ScanViewModel, d: DocMeta, index: Int,
    onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(14.dp)
    Column(Modifier.animateItem().staggerIn(index)) {
        Box {
            if (d.pages.size > 1) {
                for (k in 0..1) {
                    Box(
                        Modifier.fillMaxWidth().aspectRatio(0.76f)
                            .graphicsLayer { rotationZ = if (k == 0) -4f else 3f }
                            .background(cs.surfaceVariant, shape)
                            .border(0.5.dp, cs.outlineVariant, shape)
                    )
                }
            }
            Box(
                Modifier.fillMaxWidth().aspectRatio(0.76f)
                    .bounceClick(onLongClick = { menu = true }, onClick = onOpen)
                    .shadow(4.dp, shape)
                    .background(cs.surface, shape)
                    .border(0.5.dp, cs.outlineVariant, shape)
                    .clip(shape)
            ) {
                d.pages.firstOrNull()?.let {
                    Thumb(vm.store.outFile(d.id, it.id), Modifier.fillMaxSize(), ContentScale.Crop, 420, Alignment.TopCenter)
                }
                if (d.pages.size > 1) {
                    Text(
                        "${d.pages.size}",
                        Modifier.align(Alignment.BottomEnd).padding(8.dp).clip(CircleShape)
                            .background(Color.Black.copy(alpha = 0.55f)).padding(horizontal = 9.dp, vertical = 2.dp),
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                    )
                }
            }
            DocMenu(menu, { menu = false }, onRename, onDelete)
        }
        Spacer(Modifier.height(10.dp))
        Text(d.name, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Text(fmtDate(d.created), style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
}

@Composable
private fun androidx.compose.foundation.lazy.grid.LazyGridItemScope.DocRow(
    vm: ScanViewModel, d: DocMeta, index: Int,
    onOpen: () -> Unit, onRename: () -> Unit, onDelete: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val state = rememberSwipeToDismissBoxState(confirmValueChange = { v ->
        if (v == SwipeToDismissBoxValue.EndToStart) onDelete()
        false
    })
    SwipeToDismissBox(
        state = state,
        modifier = Modifier.animateItem().staggerIn(index).clip(RoundedCornerShape(18.dp)),
        enableDismissFromStartToEnd = false,
        backgroundContent = {
            Box(Modifier.fillMaxSize().background(cs.error), contentAlignment = Alignment.CenterEnd) {
                Icon(Icons.Outlined.Delete, "Delete", Modifier.padding(end = 24.dp), tint = Color.White)
            }
        },
    ) {
        Row(
            Modifier.fillMaxWidth().background(cs.surface)
                .bounceClick(pressedScale = 0.97f, onLongClick = { menu = true }, onClick = onOpen)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(width = 52.dp, height = 68.dp).clip(RoundedCornerShape(8.dp)).background(cs.surfaceVariant)
            ) {
                d.pages.firstOrNull()?.let {
                    Thumb(vm.store.outFile(d.id, it.id), Modifier.fillMaxSize(), ContentScale.Crop, 240, Alignment.TopCenter)
                }
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(d.name, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                val n = d.pages.size
                Text(
                    "$n ${if (n == 1) "page" else "pages"} · ${fmtDate(d.created)}",
                    style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant,
                )
            }
            Box {
                Icon(Icons.Rounded.ChevronRight, null, tint = cs.outline)
                DocMenu(menu, { menu = false }, onRename, onDelete)
            }
        }
    }
}
