package com.scanlite.app.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

/** Icon over a small label, with an animated highlight pill when selected (iOS toolbar style). */
@Composable
fun IconLabelButton(
    icon: ImageVector,
    label: String,
    selected: Boolean = false,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val bg by animateColorAsState(if (selected) cs.primaryContainer else Color.Transparent, label = "ilb-bg")
    val tint by animateColorAsState(if (selected) cs.primary else cs.onSurfaceVariant, label = "ilb-tint")
    Column(
        modifier.bounceClick(onClick = onClick).padding(horizontal = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier.size(width = 52.dp, height = 30.dp).clip(RoundedCornerShape(15.dp)).background(bg),
            contentAlignment = Alignment.Center,
        ) { Icon(icon, label, tint = tint, modifier = Modifier.size(22.dp)) }
        Spacer(Modifier.height(2.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = tint)
    }
}

/** Segmented control with a thumb that slides between options. */
@Composable
fun SlidingSegmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    BoxWithConstraints(
        modifier.fillMaxWidth().height(40.dp).clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant).padding(3.dp)
    ) {
        val seg = maxWidth / options.size
        val x by animateDpAsState(seg * selected, spring(dampingRatio = 0.8f, stiffness = 500f), label = "seg")
        Box(
            Modifier.offset(x = x).width(seg).fillMaxHeight()
                .shadow(2.dp, RoundedCornerShape(10.dp))
                .background(cs.surface, RoundedCornerShape(10.dp))
        )
        Row(Modifier.fillMaxSize()) {
            options.forEachIndexed { i, label ->
                Box(
                    Modifier.weight(1f).fillMaxHeight().clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { if (i != selected) { haptics.tick(); onSelect(i) } },
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelMedium,
                        color = if (i == selected) cs.onSurface else cs.onSurfaceVariant,
                        fontWeight = if (i == selected) FontWeight.Bold else FontWeight.Medium,
                    )
                }
            }
        }
    }
}

@Composable
fun IosSwitch(checked: Boolean, onChange: (Boolean) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val x by animateDpAsState(if (checked) 20.dp else 0.dp, spring(dampingRatio = 0.7f, stiffness = 700f), label = "sw-x")
    val track by animateColorAsState(if (checked) cs.primary else cs.outline, label = "sw-c")
    Box(
        Modifier.size(51.dp, 31.dp).clip(CircleShape).background(track)
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {
                haptics.click(); onChange(!checked)
            }
            .padding(2.dp)
    ) {
        Box(Modifier.offset(x = x).size(27.dp).shadow(2.dp, CircleShape).background(Color.White, CircleShape))
    }
}

/**
 * Ruler-style dial (like the Photos app). Drag or fling horizontally; ticks every 10 give a haptic,
 * crossing zero gives a stronger one.
 */
@Composable
fun DialSlider(value: Int, range: IntRange, onValueChange: (Int) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    val haptics = rememberHaptics()
    val unitPx = with(LocalDensity.current) { 7.dp.toPx() }
    val scope = rememberCoroutineScope()
    val emit by rememberUpdatedState(onValueChange)
    var pos by remember { mutableFloatStateOf(value.toFloat()) }
    var last by remember { mutableIntStateOf(value) }
    var flingJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(value) {
        if (value != last) { pos = value.toFloat(); last = value }
    }

    fun move(p: Float) {
        val c = p.coerceIn(range.first.toFloat(), range.last.toFloat())
        pos = c
        val r = c.roundToInt()
        if (r != last) {
            last = r
            emit(r)
            if (r == 0 && range.first < 0) haptics.confirm() else if (r % 10 == 0) haptics.tick()
        }
    }

    val dragState = rememberDraggableState { dx -> move(pos - dx / unitPx) }
    Canvas(
        modifier.fillMaxWidth().height(52.dp).draggable(
            state = dragState,
            orientation = Orientation.Horizontal,
            onDragStarted = { flingJob?.cancel() },
            onDragStopped = { v ->
                flingJob = scope.launch {
                    Animatable(pos).animateDecay(-v / unitPx, exponentialDecay(frictionMultiplier = 2f)) {
                        move(this.value)
                    }
                }
            },
        )
    ) {
        val cx = size.width / 2f
        val cy = size.height / 2f
        val half = (cx / unitPx).toInt() + 2
        val center = pos.roundToInt()
        for (i in (center - half)..(center + half)) {
            if (i < range.first || i > range.last) continue
            val x = cx + (i - pos) * unitPx
            val a = (1f - abs(x - cx) / cx * 0.85f).coerceIn(0.08f, 1f)
            val major = i % 10 == 0
            val mid = i % 5 == 0
            val h = (if (major) 26.dp else if (mid) 18.dp else 11.dp).toPx()
            drawLine(
                color = cs.onSurfaceVariant.copy(alpha = a * (if (major) 0.9f else 0.55f)),
                start = Offset(x, cy - h / 2f),
                end = Offset(x, cy + h / 2f),
                strokeWidth = (if (major) 2.dp else 1.5.dp).toPx(),
                cap = StrokeCap.Round,
            )
        }
        drawLine(cs.primary, Offset(cx, cy - 18.dp.toPx()), Offset(cx, cy + 18.dp.toPx()), 3.dp.toPx(), StrokeCap.Round)
    }
}

/** Round tool button whose ring fills to show the current value (fraction in -1..1). */
@Composable
fun AdjustRing(
    icon: ImageVector,
    label: String,
    fraction: Float,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val sweep by animateFloatAsState(fraction * 330f, spring(dampingRatio = 0.8f, stiffness = 300f), label = "ring")
    val tint by animateColorAsState(if (selected) cs.primary else cs.onSurface, label = "ring-tint")
    Column(Modifier.width(84.dp).bounceClick(onClick = onClick), horizontalAlignment = Alignment.CenterHorizontally) {
        Box(Modifier.size(58.dp), contentAlignment = Alignment.Center) {
            Canvas(Modifier.fillMaxSize()) {
                val stroke = 3.5.dp.toPx()
                val inset = stroke / 2f
                drawArc(
                    color = if (selected) cs.primary.copy(alpha = 0.22f) else cs.outlineVariant,
                    startAngle = 0f, sweepAngle = 360f, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke),
                )
                drawArc(
                    color = cs.primary,
                    startAngle = -90f, sweepAngle = sweep, useCenter = false,
                    topLeft = Offset(inset, inset), size = Size(size.width - stroke, size.height - stroke),
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            }
            Icon(icon, label, tint = tint, modifier = Modifier.size(24.dp))
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) cs.primary else cs.onSurfaceVariant,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}

@Composable
fun SearchPill(query: String, onChange: (String) -> Unit, modifier: Modifier = Modifier) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier.fillMaxWidth().height(44.dp).clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant)
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.Search, null, Modifier.size(20.dp), tint = cs.onSurfaceVariant)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) {
                Text("Search scans", style = MaterialTheme.typography.bodyLarge, color = cs.onSurfaceVariant)
            }
            BasicTextField(
                value = query,
                onValueChange = onChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
                cursorBrush = SolidColor(cs.primary),
                modifier = Modifier.fillMaxWidth(),
            )
        }
        AnimatedVisibility(query.isNotEmpty(), enter = scaleIn() + fadeIn(), exit = scaleOut() + fadeOut()) {
            Icon(
                Icons.Rounded.Close, "Clear",
                Modifier.size(20.dp).clickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                ) { onChange("") },
                tint = cs.onSurfaceVariant,
            )
        }
    }
}

/** A 0..1 value that loops forever, or a fixed 0.5 when motion is reduced. */
@Composable
fun rememberLoop(reduce: Boolean, millis: Int): Float {
    if (reduce) return 0.5f
    val t = rememberInfiniteTransition(label = "loop")
    val v by t.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(millis, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "loop-v",
    )
    return v
}

// ---------- iOS-style grouped settings ----------

@Composable
fun SettingsGroup(header: String, footer: String? = null, content: @Composable ColumnScope.() -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(
            header.uppercase(),
            Modifier.padding(start = 8.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelSmall,
            color = cs.onSurfaceVariant,
        )
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cs.surface),
            content = content,
        )
        if (footer != null) {
            Text(
                footer,
                Modifier.padding(start = 8.dp, end = 8.dp, top = 8.dp),
                style = MaterialTheme.typography.bodySmall,
                color = cs.onSurfaceVariant,
            )
        }
    }
}

@Composable
fun SettingsDivider() {
    HorizontalDivider(Modifier.padding(start = 56.dp), color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
fun SettingsTile(icon: ImageVector, color: Color) {
    Box(
        Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(color),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, Modifier.size(18.dp), tint = Color.White) }
}

/** Row with icon tile + title; [trailing] sits on the right, [below] spans the full width underneath. */
@Composable
fun SettingsRow(
    icon: ImageVector,
    color: Color,
    title: String,
    trailing: (@Composable () -> Unit)? = null,
    below: (@Composable () -> Unit)? = null,
) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            SettingsTile(icon, color)
            Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            trailing?.invoke()
        }
        if (below != null) {
            Spacer(Modifier.height(10.dp))
            below()
        }
    }
}
