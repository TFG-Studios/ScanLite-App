package com.scanlite.app.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel
import com.scanlite.app.Screen
import kotlinx.coroutines.delay

@Composable
fun ScanApp(vm: ScanViewModel) {
    val dark = when (vm.theme) {
        1 -> false
        2 -> true
        else -> isSystemInDarkTheme()
    }
    ScanTheme(dark) {
        val cs = MaterialTheme.colorScheme
        BackHandler(enabled = vm.stack.size > 1) { vm.pop() }

        Box(Modifier.fillMaxSize().background(cs.background)) {
            AnimatedContent(
                targetState = vm.stack,
                transitionSpec = {
                    if (targetState.size > initialState.size) {
                        (slideInHorizontally(tween(240)) { it / 3 } + fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(240)) { -it / 5 } + fadeOut(tween(160)))
                    } else {
                        (slideInHorizontally(tween(240)) { -it / 5 } + fadeIn(tween(240))) togetherWith
                            (slideOutHorizontally(tween(240)) { it / 3 } + fadeOut(tween(160)))
                    }
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

            if (vm.busy) {
                Box(
                    Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.25f))
                        .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {},
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(shape = CircleShape, color = cs.surface, shadowElevation = 6.dp) {
                        CircularProgressIndicator(Modifier.padding(18.dp).size(32.dp), color = cs.primary, strokeWidth = 3.dp)
                    }
                }
            }

            vm.message?.let { msg ->
                LaunchedEffect(msg) { delay(2600); vm.message = null }
                Box(Modifier.fillMaxSize().navigationBarsPadding().padding(bottom = 96.dp), contentAlignment = Alignment.BottomCenter) {
                    Surface(shape = RoundedCornerShape(22.dp), color = cs.onSurface.copy(alpha = 0.92f)) {
                        Text(
                            msg,
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
