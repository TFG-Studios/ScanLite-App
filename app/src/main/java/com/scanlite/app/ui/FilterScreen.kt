@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import androidx.compose.animation.core.animateFloat
import android.graphics.Bitmap
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CenterFocusStrong
import androidx.compose.material.icons.outlined.Compare
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.RotateRight
import androidx.compose.material.icons.outlined.Tonality
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel
import com.scanlite.app.imaging.Bmp
import com.scanlite.app.imaging.Filters
import com.scanlite.app.imaging.Processor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun FilterScreen(vm: ScanViewModel) {
    val base = vm.base ?: return
    val p = vm.params
    val cs = MaterialTheme.colorScheme
    val reduce = LocalReduceMotion.current

    var preview by remember { mutableStateOf<Bitmap?>(null) }
    var thumbs by remember { mutableStateOf<List<Bitmap>>(emptyList()) }
    var processing by remember { mutableStateOf(false) }
    var tab by remember { mutableIntStateOf(0) }      // 0 filters, 1 adjust
    var tool by remember { mutableIntStateOf(0) }     // 0 brightness, 1 contrast, 2 sharpness
    var comparing by remember { mutableStateOf(false) }

    // zoom / pan of the preview
    var zoom by remember { mutableFloatStateOf(1f) }
    var pan by remember { mutableStateOf(Offset.Zero) }
    var box by remember { mutableStateOf(IntSize.Zero) }

    val original = remember(base, p.rotation) { Bmp.rotate(base, p.rotation) }

    LaunchedEffect(base, p) {
        processing = true
        delay(40)
        preview = withContext(Dispatchers.Default) { Processor.process(base, p) }
        processing = false
    }
    LaunchedEffect(base, p.rotation) {
        thumbs = withContext(Dispatchers.Default) {
            val s = Processor.shrink(base, 150)
            Filters.names.indices.map { Processor.process(s, Filters.defaults(it).copy(rotation = p.rotation)) }
        }
    }

    Column(Modifier.fillMaxSize()) {
        NavBar("Enhance", onBack = { vm.pop() }) {
            RoundIconButton(Icons.Outlined.RestartAlt, "Reset") {
                vm.params = Filters.defaults(p.filter).copy(rotation = p.rotation)
            }
            RoundIconButton(Icons.Outlined.RotateRight, "Rotate") {
                vm.params = vm.params.copy(rotation = (vm.params.rotation + 90) % 360)
            }
        }

        // ---------- preview ----------
        Box(
            Modifier.weight(1f).fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                .onSizeChanged { box = it }
                .clipToBounds()
                .pointerInput(Unit) {
                    detectTransformGestures { _, panDelta, zoomDelta, _ ->
                        zoom = (zoom * zoomDelta).coerceIn(1f, 5f)
                        val mx = (zoom - 1f) * box.width / 2f
                        val my = (zoom - 1f) * box.height / 2f
                        pan = Offset(
                            (pan.x + panDelta.x).coerceIn(-mx, mx),
                            (pan.y + panDelta.y).coerceIn(-my, my),
                        )
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures(onDoubleTap = { zoom = 1f; pan = Offset.Zero })
                },
            contentAlignment = Alignment.Center,
        ) {
            val shown: Bitmap? = if (comparing) original else preview
            if (shown == null) {
                val a = if (reduce) 0.5f else {
                    val t = rememberInfiniteTransition(label = "pulse")
                    val v by t.animateFloat(
                        0.35f, 0.7f, infiniteRepeatable(tween(800), RepeatMode.Reverse), label = "pulse-a",
                    )
                    v
                }
                Box(
                    Modifier.fillMaxSize().padding(24.dp).clip(RoundedCornerShape(16.dp))
                        .background(cs.surfaceVariant.copy(alpha = a))
                )
            } else {
                Crossfade(
                    targetState = shown,
                    animationSpec = if (reduce) snap() else tween(160),
                    label = "preview",
                ) { bmp ->
                    val ib = remember(bmp) { bmp.asImageBitmap() }
                    Image(
                        ib, null,
                        Modifier.fillMaxSize().graphicsLayer {
                            scaleX = zoom; scaleY = zoom
                            translationX = pan.x; translationY = pan.y
                        },
                        contentScale = ContentScale.Fit,
                    )
                }
            }

            // hold to compare with the original
            Box(
                Modifier.align(Alignment.BottomStart).padding(6.dp).size(46.dp).clip(CircleShape)
                    .background(cs.surface.copy(alpha = 0.92f))
                    .pointerInput(Unit) {
                        detectTapGestures(onPress = {
                            comparing = true
                            tryAwaitRelease()
                            comparing = false
                        })
                    },
                contentAlignment = Alignment.Center,
            ) { Icon(Icons.Outlined.Compare, "Hold to compare", tint = cs.primary) }

            if (processing && preview != null) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(6.dp).clip(CircleShape)
                        .background(cs.surface.copy(alpha = 0.92f)).padding(8.dp)
                ) { CircularProgressIndicator(Modifier.size(18.dp), color = cs.primary, strokeWidth = 2.dp) }
            }
        }

        // ---------- tool panel ----------
        Surface(
            color = cs.surface,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            shadowElevation = 10.dp,
        ) {
            Column(Modifier.navigationBarsPadding().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp)) {
                Box(Modifier.fillMaxWidth().height(164.dp)) {
                    if (tab == 0) {
                        FiltersPane(vm, p, thumbs)
                    } else {
                        AdjustPane(vm, p, tool) { tool = it }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconLabelButton(Icons.Outlined.AutoAwesome, "Filters", tab == 0) { tab = 0 }
                    IconLabelButton(Icons.Outlined.Tune, "Adjust", tab == 1) { tab = 1 }
                    Spacer(Modifier.weight(1f))
                    if (!vm.isEditing && vm.canAddMore) {
                        Box {
                            RoundIconButton(
                                Icons.Rounded.Add, "Add page",
                                Modifier.size(48.dp), container = cs.primaryContainer,
                            ) { vm.savePage(false) }
                            if (vm.queueLeft > 0) {
                                Text(
                                    "${vm.queueLeft}",
                                    Modifier.align(Alignment.TopEnd).clip(CircleShape).background(cs.primary)
                                        .padding(horizontal = 6.dp),
                                    color = cs.onPrimary,
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                        }
                        Spacer(Modifier.size(8.dp))
                    }
                    PillButton(if (vm.isEditing) "Save" else "Done", Icons.Rounded.Check) { vm.savePage(true) }
                }
            }
        }
    }
}

@Composable
private fun FiltersPane(vm: ScanViewModel, p: com.scanlite.app.imaging.EditParams, thumbs: List<Bitmap>) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        LazyRow(
            Modifier.fillMaxWidth().height(100.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            itemsIndexed(Filters.names) { i, name ->
                FilterCard(name, thumbs.getOrNull(i), p.filter == i) {
                    vm.params = Filters.defaults(i).copy(rotation = vm.params.rotation)
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        if (p.filter == Filters.ORIGINAL) {
            Box(Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                Text(
                    "Pick a filter to adjust its intensity",
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            ValueLine("Intensity", p.strength)
            DialSlider(p.strength, 0..100, { vm.params = vm.params.copy(strength = it) })
        }
    }
}

@Composable
private fun AdjustPane(vm: ScanViewModel, p: com.scanlite.app.imaging.EditParams, tool: Int, onTool: (Int) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            AdjustRing(Icons.Outlined.WbSunny, "Brightness", p.brightness / 100f, tool == 0) { onTool(0) }
            AdjustRing(Icons.Outlined.Tonality, "Contrast", p.contrast / 100f, tool == 1) { onTool(1) }
            AdjustRing(Icons.Outlined.CenterFocusStrong, "Sharpness", p.sharpness / 100f, tool == 2) { onTool(2) }
        }
        Spacer(Modifier.height(6.dp))
        when (tool) {
            0 -> {
                ValueLine("Brightness", p.brightness)
                DialSlider(p.brightness, -100..100, { vm.params = vm.params.copy(brightness = it) })
            }
            1 -> {
                ValueLine("Contrast", p.contrast)
                DialSlider(p.contrast, -100..100, { vm.params = vm.params.copy(contrast = it) })
            }
            else -> {
                ValueLine("Sharpness", p.sharpness)
                DialSlider(p.sharpness, 0..100, { vm.params = vm.params.copy(sharpness = it) })
            }
        }
    }
}

@Composable
private fun ValueLine(label: String, value: Int) {
    val cs = MaterialTheme.colorScheme
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        Spacer(Modifier.size(8.dp))
        Text(
            if (value > 0 && label != "Intensity" && label != "Sharpness") "+$value" else "$value",
            style = MaterialTheme.typography.labelMedium,
            color = cs.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Composable
private fun FilterCard(name: String, thumb: Bitmap?, selected: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val scale by animateFloatAsState(
        if (selected) 1.07f else 1f, spring(dampingRatio = 0.6f, stiffness = 500f), label = "filter-scale",
    )
    val shape = RoundedCornerShape(12.dp)
    Column(Modifier.bounceClick(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            Modifier.graphicsLayer { scaleX = scale; scaleY = scale }
                .size(60.dp, 76.dp).clip(shape).background(cs.surfaceVariant)
                .then(if (selected) Modifier.border(2.5.dp, cs.primary, shape) else Modifier)
        ) {
            thumb?.let {
                val ib = remember(it) { it.asImageBitmap() }
                Image(ib, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            name,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) cs.primary else cs.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
