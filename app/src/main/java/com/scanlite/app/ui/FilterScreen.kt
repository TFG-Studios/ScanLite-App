@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.RotateRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel
import com.scanlite.app.imaging.Filters
import com.scanlite.app.imaging.Processor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun FilterScreen(vm: ScanViewModel) {
    val base = vm.base ?: return
    val p = vm.params
    val cs = MaterialTheme.colorScheme
    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var thumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var tab by remember { mutableIntStateOf(0) }

    // Re-render the preview shortly after the last change (cancelled automatically if params change again).
    LaunchedEffect(base, p) {
        delay(50)
        preview = withContext(Dispatchers.Default) { Processor.process(base, p) }
    }
    LaunchedEffect(base, p.rotation) {
        thumbs = withContext(Dispatchers.Default) {
            val s = Processor.shrink(base, 150)
            Filters.names.indices.map { Processor.process(s, Filters.defaults(it).copy(rotation = p.rotation)) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        NavBar("Enhance", onBack = { vm.pop() }) {
            RoundIconButton(Icons.Rounded.RotateRight, "Rotate") {
                vm.params = p.copy(rotation = (p.rotation + 90) % 360)
            }
        }
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center,
        ) {
            preview?.let {
                Image(it.asImageBitmap(), null, Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
            }
        }

        Surface(
            color = cs.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        ) {
            Column(Modifier.navigationBarsPadding().padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Segmented(listOf("Filters", "Adjust"), tab, { tab = it }, Modifier.weight(1f))
                    if (tab == 1) {
                        TextButton({ vm.params = Filters.defaults(p.filter).copy(rotation = p.rotation) }) { Text("Reset") }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.height(112.dp).fillMaxWidth()) {
                    if (tab == 0) {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            itemsIndexed(Filters.names) { i, name ->
                                val selected = p.filter == i
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Surface(
                                        onClick = { vm.params = Filters.defaults(i).copy(rotation = p.rotation) },
                                        shape = RoundedCornerShape(12.dp),
                                        color = cs.surfaceVariant,
                                        border = if (selected) BorderStroke(2.5.dp, cs.primary) else null,
                                        modifier = Modifier.size(width = 68.dp, height = 84.dp),
                                    ) {
                                        thumbs.getOrNull(i)?.let {
                                            Image(
                                                it.asImageBitmap(), null,
                                                Modifier.fillMaxSize().padding(3.dp).clip(RoundedCornerShape(9.dp)),
                                                contentScale = ContentScale.Crop,
                                            )
                                        }
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        name,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) cs.primary else cs.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    } else {
                        Column(Modifier.fillMaxSize(), verticalArrangement = Arrangement.SpaceEvenly) {
                            AdjustRow("Brightness", p.brightness, -100f..100f) { vm.params = p.copy(brightness = it) }
                            AdjustRow("Contrast", p.contrast, -100f..100f) { vm.params = p.copy(contrast = it) }
                            AdjustRow("Sharpness", p.sharpness, 0f..100f) { vm.params = p.copy(sharpness = it) }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    if (!vm.isEditing && vm.canAddMore) {
                        PillButton(
                            if (vm.queueLeft > 0) "Next (${vm.queueLeft})" else "Add page",
                            Icons.Rounded.Add, Modifier.weight(1f), filled = false,
                        ) { vm.savePage(false) }
                    }
                    PillButton(if (vm.isEditing) "Save" else "Done", Icons.Rounded.Check, Modifier.weight(1f)) {
                        vm.savePage(true)
                    }
                }
            }
        }
    }
}

@Composable
private fun AdjustRow(label: String, value: Int, range: ClosedFloatingPointRange<Float>, onChange: (Int) -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(label, Modifier.width(88.dp), style = MaterialTheme.typography.bodyMedium)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range,
            modifier = Modifier.weight(1f),
        )
        Text(
            "$value", Modifier.width(36.dp), textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
