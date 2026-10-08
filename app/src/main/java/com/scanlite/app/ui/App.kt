package com.scanlite.app.ui

import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel
import com.scanlite.app.Screen
import kotlinx.coroutines.delay

@Composable
fun ScanApp(vm: ScanViewModel) {
    val ctx = LocalContext.current
    val dark = when (vm.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    val systemAnimationsOff = remember {
        try {
            Settings.Global.getFloat(ctx.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        } catch (e: Exception) {
            false
        }
    }
    val reduce = vm.motion == 2 || (vm.motion == 0 && systemAnimationsOff)

    ScanTheme(dark) {
        CompositionLocalProvider(LocalReduceMotion provides reduce, LocalHaptics provides vm.haptics) {
            val cs = MaterialTheme.colorScheme
            BackHandler(enabled = vm.stack.size > 1) { vm.pop() }

            Box(Modifier.fillMaxSize().background(cs.background)) {
                AnimatedContent(
                    targetState = vm.stack,
                    transitionSpec = {
                        val forward = targetState.size > initialState.size
                        val t = if (forward) {
                            if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                            else (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { -it / 5 } + fadeOut(tween(200)))
                        } else {
                            if (reduce) fadeIn(tween(120)) togetherWith fadeOut(tween(120))
                            else (slideInHorizontally(tween(300)) { -it / 5 } + fadeIn(tween(300))) togetherWith
                                (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)))
                        }
                        t.apply { targetContentZIndex = if (forward) 1f else -1f }
                    },
                    label = "nav",
                ) { st ->
                    Box(Modifier.fillMaxSize().background(cs.background)) {
                        when (val s = st.last()) {
                            Screen.Home -> HomeScreen(vm)
                            Screen.Camera -> CameraScreen(vm)
                            Screen.Crop -> CropScreen(vm)
                            Screen.Filter -> FilterScreen(vm)
                            is Screen.Doc -> DocScreen(vm, s.id)
                            Screen.Settings -> SettingsScreen(vm)
                        }
                    }
                }

                AnimatedVisibility(
                    visible = vm.busy,
                    enter = fadeIn(tween(150)),
                    exit = fadeOut(tween(150)),
                ) {
                    Box(
                        Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))
                            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                        contentAlignment = Alignment.Center,
                    ) {
                        Surface(shape = CircleShape, color = cs.surface, shadowElevation = 8.dp) {
                            CircularProgressIndicator(
                                Modifier.padding(18.dp).size(32.dp), color = cs.primary, strokeWidth = 3.dp,
                            )
                        }
                    }
                }

                val msg = vm.message
                var lastMsg by remember { mutableStateOf("") }
                if (msg != null && msg != lastMsg) lastMsg = msg
                LaunchedEffect(msg) {
                    if (msg != null) {
                        delay(2600)
                        vm.message = null
                    }
                }
                Box(
                    Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 104.dp),
                    contentAlignment = Alignment.BottomCenter,
                ) {
                    AnimatedVisibility(
                        visible = msg != null,
                        enter = slideInVertically(tween(260)) { it / 2 } + fadeIn(tween(200)),
                        exit = slideOutVertically(tween(200)) { it / 2 } + fadeOut(tween(160)),
                    ) {
                        Surface(shape = RoundedCornerShape(22.dp), color = cs.onSurface.copy(alpha = 0.92f)) {
                            Text(
                                lastMsg,
                                Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                                color = cs.surface,
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
