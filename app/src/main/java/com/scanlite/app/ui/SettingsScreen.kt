@file:OptIn(ExperimentalMaterial3Api::class)

package com.scanlite.app.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel

@Composable
fun SettingsScreen(vm: ScanViewModel) {
    Column(Modifier.fillMaxSize()) {
        NavBar("Settings", onBack = { vm.pop() })
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding().padding(16.dp),
            verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(20.dp),
        ) {
            Group("Appearance") {
                Segmented(listOf("System", "Light", "Dark"), vm.theme, vm::setTheme)
            }
            Group(
                "Scan quality",
                "Fast saves battery, memory and storage on older phones. Sharp is best for small print.",
            ) {
                Segmented(listOf("Fast", "Balanced", "Sharp"), vm.quality, vm::setQuality)
            }
            Group("PDF page size", "A4 centers each page on a standard sheet. Original keeps the scan's own proportions.") {
                Segmented(listOf("A4", "Original"), vm.pdfSize, vm::setPdfSize)
            }
            Group("About") {
                Text(
                    "ScanLite 1.0.0\nWorks 100% offline. The app has no internet permission, no accounts and no tracking. " +
                        "Your scans never leave this phone unless you share them.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun Group(title: String, caption: String? = null, content: @Composable () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Column {
        Text(
            title,
            Modifier.padding(start = 6.dp, bottom = 8.dp),
            style = MaterialTheme.typography.labelMedium,
            color = cs.onSurfaceVariant,
        )
        Surface(shape = RoundedCornerShape(18.dp), color = cs.surface, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                content()
                if (caption != null) {
                    Spacer(Modifier.height(10.dp))
                    Text(caption, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                }
            }
        }
    }
}
