@file:OptIn(ExperimentalFoundationApi::class)

package com.scanlite.app.ui

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.delay

/** True when the user (or the phone's "remove animations" setting) asks for less motion. */
val LocalReduceMotion = compositionLocalOf { false }
val LocalHaptics = compositionLocalOf { true }

class Haptics(private val view: View, private val enabled: Boolean) {
    fun tick() { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK) }
    fun click() { if (enabled) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP) }
    fun confirm() {
        if (enabled) {
            view.performHapticFeedback(
                if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM
                else HapticFeedbackConstants.LONG_PRESS
            )
        }
    }
}

@Composable
fun rememberHaptics(): Haptics {
    val view = LocalView.current
    val on = LocalHaptics.current
    return remember(view, on) { Haptics(view, on) }
}

/** Springy press feedback + light haptic. Replaces the default ripple everywhere. */
fun Modifier.bounceClick(
    enabled: Boolean = true,
    pressedScale: Float = 0.93f,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
): Modifier = composed {
    val reduce = LocalReduceMotion.current
    val haptics = rememberHaptics()
    val source = remember { MutableInteractionSource() }
    val pressed by source.collectIsPressedAsState()
    val s by animateFloatAsState(
        targetValue = if (pressed && !reduce) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "bounce",
    )
    this
        .graphicsLayer { scaleX = s; scaleY = s }
        .combinedClickable(
            interactionSource = source,
            indication = null,
            enabled = enabled,
            onLongClick = if (onLongClick == null) null else ({ haptics.confirm(); onLongClick() }),
            onClick = { haptics.click(); onClick() },
        )
}

/** Fade + rise entrance, staggered by index (only the first 8 items animate). */
@Composable
fun Modifier.staggerIn(index: Int): Modifier {
    val reduce = LocalReduceMotion.current
    var shown by remember { mutableStateOf(reduce || index > 7) }
    LaunchedEffect(Unit) {
        if (!shown) {
            delay(index * 40L)
            shown = true
        }
    }
    val p by animateFloatAsState(if (shown) 1f else 0f, tween(340), label = "stagger")
    return this.graphicsLayer {
        alpha = p
        translationY = (1f - p) * 36f
    }
}
