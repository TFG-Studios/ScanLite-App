package com.scanlite.app.ui

import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.util.LruCache
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.scanlite.app.imaging.Bmp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

tailrec fun Context.findActivity(): ComponentActivity? = when (this) {
    is ComponentActivity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}

fun fmtDate(ms: Long): String = SimpleDateFormat("d MMM yyyy", Locale.getDefault()).format(Date(ms))

/** Small in-memory thumbnail cache (capped at 12 MB so it is safe on low-RAM phones). */
object Thumbs {
    private val cache = object : LruCache<String, Bitmap>(12 * 1024 * 1024) {
        override fun sizeOf(key: String, value: Bitmap) = value.byteCount
    }

    suspend fun get(f: File, px: Int): Bitmap? {
        val key = "${f.path}|${f.lastModified()}|$px"
        cache.get(key)?.let { return it }
        return withContext(Dispatchers.IO) { Bmp.decodeFile(f, px)?.also { cache.put(key, it) } }
    }
}

@Composable
fun Thumb(
    file: File,
    modifier: Modifier = Modifier,
    scale: ContentScale = ContentScale.Fit,
    px: Int = 360,
    alignment: Alignment = Alignment.Center,
) {
    val bmp by produceState<Bitmap?>(null, file.path, file.lastModified()) { value = Thumbs.get(file, px) }
    val a by animateFloatAsState(if (bmp != null) 1f else 0f, tween(220), label = "thumb-fade")
    bmp?.let {
        val ib = remember(it) { it.asImageBitmap() }
        Image(ib, null, modifier.graphicsLayer { alpha = a }, contentScale = scale, alignment = alignment)
    }
}

@Composable
fun RoundIconButton(
    icon: ImageVector,
    desc: String,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.primary,
    container: Color = MaterialTheme.colorScheme.surfaceVariant,
    onClick: () -> Unit,
) {
    Box(
        modifier.size(40.dp).bounceClick(onClick = onClick).clip(CircleShape).background(container),
        contentAlignment = Alignment.Center,
    ) { Icon(icon, desc, tint = tint, modifier = Modifier.size(22.dp)) }
}

@Composable
fun PillButton(
    text: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    filled: Boolean = true,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    val cs = MaterialTheme.colorScheme
    val fill: Brush = if (filled) {
        Brush.verticalGradient(listOf(lerp(cs.primary, Color.White, 0.18f), cs.primary))
    } else SolidColor(cs.primaryContainer)
    val content = if (filled) cs.onPrimary else cs.onPrimaryContainer
    Row(
        modifier.height(52.dp).alpha(if (enabled) 1f else 0.4f)
            .bounceClick(enabled = enabled, onClick = onClick)
            .clip(RoundedCornerShape(26.dp)).background(fill).padding(horizontal = 22.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(icon, null, Modifier.size(22.dp), tint = content)
            Spacer(Modifier.width(8.dp))
        }
        Text(text, style = MaterialTheme.typography.labelLarge, color = content, maxLines = 1)
    }
}

/** iOS-style navigation bar: round back button, centered title, optional actions. */
@Composable
fun NavBar(
    title: String,
    onBack: (() -> Unit)?,
    modifier: Modifier = Modifier,
    actions: @Composable RowScope.() -> Unit = {},
) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().height(56.dp).padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.widthIn(min = 44.dp), contentAlignment = Alignment.CenterStart) {
            if (onBack != null) RoundIconButton(Icons.AutoMirrored.Rounded.ArrowBack, "Back") { onBack() }
        }
        Text(
            title,
            Modifier.weight(1f).padding(horizontal = 8.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Row(
            Modifier.widthIn(min = 44.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
            content = actions,
        )
    }
}

@Composable
fun Segmented(options: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) =
    SlidingSegmented(options, selected, onSelect, modifier)

/** Centered, spring-in alert in iOS style. */
@Composable
fun IosAlert(
    title: String,
    confirmText: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    message: String? = null,
    destructive: Boolean = false,
    content: (@Composable () -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        var shown by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) { shown = true }
        val s by animateFloatAsState(if (shown) 1f else 0.86f, spring(dampingRatio = 0.7f, stiffness = 600f), label = "alert-s")
        val a by animateFloatAsState(if (shown) 1f else 0f, tween(160), label = "alert-a")
        Surface(
            Modifier.padding(horizontal = 32.dp).widthIn(max = 320.dp).fillMaxWidth()
                .graphicsLayer { scaleX = s; scaleY = s; alpha = a },
            shape = RoundedCornerShape(22.dp),
            color = cs.surface,
        ) {
            Column {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
                    if (message != null) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            message, style = MaterialTheme.typography.bodyMedium,
                            color = cs.onSurfaceVariant, textAlign = TextAlign.Center,
                        )
                    }
                    if (content != null) {
                        Spacer(Modifier.height(14.dp))
                        content()
                    }
                }
                HorizontalDivider(color = cs.outlineVariant)
                Row(Modifier.fillMaxWidth().height(52.dp)) {
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onDismiss),
                        contentAlignment = Alignment.Center,
                    ) { Text("Cancel", color = cs.primary, style = MaterialTheme.typography.bodyLarge) }
                    VerticalDivider(color = cs.outlineVariant)
                    Box(
                        Modifier.weight(1f).fillMaxHeight().clickable(onClick = onConfirm),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            confirmText,
                            color = if (destructive) cs.error else cs.primary,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun RenameDialog(initial: String, onDone: (String) -> Unit, onDismiss: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var text by remember { mutableStateOf(initial) }
    IosAlert(
        title = "Rename scan",
        confirmText = "Save",
        onConfirm = { onDone(text) },
        onDismiss = onDismiss,
    ) {
        BasicTextField(
            value = text,
            onValueChange = { text = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = cs.onSurface),
            cursorBrush = SolidColor(cs.primary),
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(cs.surfaceVariant).padding(12.dp),
        )
    }
}

@Composable
fun ConfirmDialog(title: String, body: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    IosAlert(
        title = title,
        confirmText = confirm,
        onConfirm = onConfirm,
        onDismiss = onDismiss,
        message = body,
        destructive = true,
    )
}
