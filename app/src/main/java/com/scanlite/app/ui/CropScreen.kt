@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CropFree
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel
import kotlin.math.hypot
import kotlin.math.roundToInt

@Composable
fun CropScreen(vm: ScanViewModel) {
    val raw = vm.raw ?: return
    val primary = MaterialTheme.colorScheme.primary
    val image = remember(raw) { raw.asImageBitmap() }
    var active by remember { mutableIntStateOf(-1) }

    Column(Modifier.fillMaxSize()) {
        NavBar("Adjust corners", onBack = { vm.pop() })
        BoxWithConstraints(Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
            val bw = constraints.maxWidth.toFloat()
            val bh = constraints.maxHeight.toFloat()
            val scale = minOf(bw / image.width, bh / image.height)
            val dw = image.width * scale
            val dh = image.height * scale
            val ox = (bw - dw) / 2f
            val oy = (bh - dh) / 2f

            Canvas(
                Modifier.fillMaxSize().pointerInput(raw, bw, bh) {
                    val grab = 72.dp.toPx()
                    detectDragGestures(
                        onDragStart = { o ->
                            val c = vm.corners
                            var best = -1
                            var bd = Float.MAX_VALUE
                            for (i in 0..3) {
                                val d = hypot(ox + c[i * 2] * dw - o.x, oy + c[i * 2 + 1] * dh - o.y)
                                if (d < bd) { bd = d; best = i }
                            }
                            active = if (bd < grab) best else -1
                        },
                        onDrag = { change, delta ->
                            val i = active
                            if (i >= 0) {
                                change.consume()
                                val c = vm.corners.toMutableList()
                                c[i * 2] = (c[i * 2] + delta.x / dw).coerceIn(0f, 1f)
                                c[i * 2 + 1] = (c[i * 2 + 1] + delta.y / dh).coerceIn(0f, 1f)
                                vm.corners = c
                            }
                        },
                        onDragEnd = { active = -1 },
                        onDragCancel = { active = -1 },
                    )
                }
            ) {
                drawImage(
                    image,
                    dstOffset = IntOffset(ox.roundToInt(), oy.roundToInt()),
                    dstSize = IntSize(dw.roundToInt(), dh.roundToInt()),
                    filterQuality = FilterQuality.Medium,
                )
                val c = vm.corners
                val q = (0..3).map { Offset(ox + c[it * 2] * dw, oy + c[it * 2 + 1] * dh) }
                val quad = Path().apply {
                    moveTo(q[0].x, q[0].y)
                    lineTo(q[1].x, q[1].y)
                    lineTo(q[2].x, q[2].y)
                    lineTo(q[3].x, q[3].y)
                    close()
                }
                val outer = Path().apply {
                    addRect(Rect(ox, oy, ox + dw, oy + dh))
                    addPath(quad)
                    fillType = PathFillType.EvenOdd
                }
                drawPath(outer, Color.Black.copy(alpha = 0.5f))
                drawPath(quad, primary, style = Stroke(width = 2.5.dp.toPx(), join = StrokeJoin.Round))
                q.forEachIndexed { i, p ->
                    if (i == active) drawCircle(primary.copy(alpha = 0.3f), 28.dp.toPx(), p)
                    drawCircle(Color.White, 13.dp.toPx(), p)
                    drawCircle(primary, 13.dp.toPx(), p, style = Stroke(width = 3.dp.toPx()))
                }
            }
        }
        Row(
            Modifier.fillMaxWidth().navigationBarsPadding().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            PillButton("Auto", Icons.Rounded.AutoFixHigh, Modifier.weight(1f), filled = false) { vm.autoDetect() }
            PillButton("Full", Icons.Rounded.CropFree, Modifier.weight(1f), filled = false) { vm.selectAll() }
            PillButton("Next", Icons.Rounded.Check, Modifier.weight(1.2f)) { vm.confirmCrop() }
        }
    }
}
