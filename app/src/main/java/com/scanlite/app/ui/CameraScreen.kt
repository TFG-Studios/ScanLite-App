@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.util.Size
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FlashOff
import androidx.compose.material.icons.rounded.FlashOn
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import com.scanlite.app.ScanViewModel
import java.io.File

@Composable
fun CameraScreen(vm: ScanViewModel) {
    val ctx = LocalContext.current
    val view = LocalView.current
    val cs = MaterialTheme.colorScheme
    val owner = ctx.findActivity() ?: return
    val themeIsDark = cs.background.luminance() < 0.5f

    var granted by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(ctx, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED)
    }
    val permLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted = it }
    val gallery = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) vm.onImages(uris)
    }
    LaunchedEffect(Unit) { if (!granted) permLauncher.launch(Manifest.permission.CAMERA) }

    // Light status-bar icons over the black viewfinder; restore on exit.
    DisposableEffect(Unit) {
        val c = WindowCompat.getInsetsController(owner.window, view)
        c.isAppearanceLightStatusBars = false
        onDispose { c.isAppearanceLightStatusBars = !themeIsDark }
    }

    if (!granted) {
        Column(
            Modifier.fillMaxSize().padding(32.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Icon(Icons.Rounded.CameraAlt, null, Modifier.size(56.dp), tint = cs.primary)
            Spacer(Modifier.height(16.dp))
            Text("Camera access needed", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(6.dp))
            Text(
                "ScanLite uses the camera only to photograph your documents. Nothing leaves your phone.",
                style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))
            PillButton("Allow camera") { permLauncher.launch(Manifest.permission.CAMERA) }
            Spacer(Modifier.height(10.dp))
            PillButton("Open settings", filled = false) {
                ctx.startActivity(
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))
                )
            }
            Spacer(Modifier.height(10.dp))
            PillButton("Import from gallery", Icons.Rounded.PhotoLibrary, filled = false) { gallery.launch("image/*") }
            Spacer(Modifier.height(10.dp))
            PillButton("Back", filled = false) { vm.pop() }
        }
        return
    }

    var torch by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<Camera?>(null) }
    val previewView = remember {
        PreviewView(ctx).apply { scaleType = PreviewView.ScaleType.FIT_CENTER }
    }
    val imageCapture = remember {
        ImageCapture.Builder()
            .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
            .setResolutionSelector(selector(2560, 1920))
            .build()
    }

    DisposableEffect(owner) {
        val future = ProcessCameraProvider.getInstance(ctx)
        future.addListener({
            try {
                val provider = future.get()
                val preview = Preview.Builder().setResolutionSelector(selector(1280, 960)).build()
                    .also { it.setSurfaceProvider(previewView.surfaceProvider) }
                provider.unbindAll()
                camera = provider.bindToLifecycle(owner, CameraSelector.DEFAULT_BACK_CAMERA, preview, imageCapture)
            } catch (e: Exception) {
                vm.message = "Camera unavailable"
            }
        }, ContextCompat.getMainExecutor(ctx))
        onDispose { runCatching { future.get().unbindAll() } }
    }
    LaunchedEffect(camera, torch) { camera?.cameraControl?.enableTorch(torch) }

    fun capture() {
        if (vm.busy) return
        val file = File(ctx.cacheDir, "capture/cap_${System.currentTimeMillis()}.jpg").apply { parentFile?.mkdirs() }
        imageCapture.takePicture(
            ImageCapture.OutputFileOptions.Builder(file).build(),
            ContextCompat.getMainExecutor(ctx),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    vm.onImages(listOf(Uri.fromFile(file)))
                }

                override fun onError(exception: ImageCaptureException) {
                    vm.message = "Capture failed, try again"
                }
            },
        )
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView({ previewView }, Modifier.fillMaxSize())

        Row(
            Modifier.fillMaxWidth().statusBarsPadding().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CamButton(Icons.Rounded.Close) { vm.pop() }
            if (camera?.cameraInfo?.hasFlashUnit() == true) {
                CamButton(if (torch) Icons.Rounded.FlashOn else Icons.Rounded.FlashOff) { torch = !torch }
            }
        }
        Text(
            "Fill the frame with the page",
            Modifier.align(Alignment.TopCenter).statusBarsPadding().padding(top = 72.dp)
                .clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).padding(horizontal = 14.dp, vertical = 6.dp),
            color = Color.White, style = MaterialTheme.typography.labelMedium,
        )

        Row(
            Modifier.align(Alignment.BottomCenter).fillMaxWidth().navigationBarsPadding()
                .padding(start = 32.dp, end = 32.dp, bottom = 28.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CamButton(Icons.Rounded.PhotoLibrary) { gallery.launch("image/*") }
            Box(
                Modifier.size(78.dp).border(4.dp, Color.White, CircleShape).padding(7.dp)
                    .clip(CircleShape).background(Color.White).clickable { capture() },
            )
            Spacer(Modifier.size(44.dp))
        }
    }
}

private fun selector(w: Int, h: Int) = ResolutionSelector.Builder()
    .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
    .setResolutionStrategy(ResolutionStrategy(Size(w, h), ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER))
    .build()

@Composable
private fun CamButton(icon: ImageVector, onClick: () -> Unit) {
    Box(
        Modifier.size(44.dp).clip(CircleShape).background(Color.Black.copy(alpha = 0.45f)).clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, null, tint = Color.White, modifier = Modifier.size(24.dp)) }
}
