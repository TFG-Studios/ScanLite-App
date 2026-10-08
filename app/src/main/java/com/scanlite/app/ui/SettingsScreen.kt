package com.scanlite.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PictureAsPdf
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.scanlite.app.ScanViewModel

private val Cyan = Color(0xFF1FB5D6)
private val Indigo = Color(0xFF5E5CE6)
private val Orange = Color(0xFFFF9F0A)
private val Green = Color(0xFF30B85C)
private val Red = Color(0xFFFF453A)
private val Blue = Color(0xFF0A84FF)
private val Gray = Color(0xFF8E8E93)

@Composable
fun SettingsScreen(vm: ScanViewModel) {
    val cs = MaterialTheme.colorScheme
    Column(Modifier.fillMaxSize()) {
        NavBar("Settings", onBack = { vm.pop() })
        Column(
            Modifier.verticalScroll(rememberScrollState()).navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(26.dp),
        ) {
            SettingsGroup("Appearance", "Auto follows your phone's “remove animations” setting.") {
                SettingsRow(Icons.Outlined.Palette, Cyan, "Theme", below = {
                    SlidingSegmented(listOf("System", "Light", "Dark"), vm.theme, vm::changeTheme)
                })
                SettingsDivider()
                SettingsRow(Icons.Outlined.Speed, Indigo, "Animations", below = {
                    SlidingSegmented(listOf("Auto", "Full", "Reduced"), vm.motion, vm::changeMotion)
                })
                SettingsDivider()
                SettingsRow(
                    Icons.Outlined.Vibration, Orange, "Haptic feedback",
                    trailing = { IosSwitch(vm.haptics, vm::changeHaptics) },
                )
            }
            SettingsGroup(
                "Scanning",
                "Fast saves battery, memory and storage on older phones. Sharp is best for small print. " +
                    "A4 centers each page on a standard sheet; Original keeps the scan's own proportions.",
            ) {
                SettingsRow(Icons.Outlined.Tune, Green, "Scan quality", below = {
                    SlidingSegmented(listOf("Fast", "Balanced", "Sharp"), vm.quality, vm::changeQuality)
                })
                SettingsDivider()
                SettingsRow(Icons.Outlined.PictureAsPdf, Red, "PDF page size", below = {
                    SlidingSegmented(listOf("A4", "Original"), vm.pdfSize, vm::changePdfSize)
                })
            }
            SettingsGroup(
                "About",
                "No internet permission, no accounts, no tracking. Typeface: Poppins (SIL Open Font License 1.1).",
            ) {
                SettingsRow(Icons.Outlined.Lock, Blue, "Privacy", trailing = {
                    Text("100% offline", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                })
                SettingsDivider()
                SettingsRow(Icons.Outlined.Info, Gray, "Version", trailing = {
                    Text("1.1.0", style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
                })
            }
        }
    }
}
