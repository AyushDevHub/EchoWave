package com.howdy.echowave.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Feedback
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.BuildConfig
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.data.local.FontChoice
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.data.local.ThemePreset
import com.howdy.echowave.ui.components.AppLogo

@Composable
fun SettingsScreen(
    onOpenAppearance: () -> Unit = {},
    onOpenPlayback: () -> Unit = {},
    onOpenAbout: () -> Unit = {},
    onEditTaste: () -> Unit = {},
    onOpenListeningSummary: () -> Unit = {},
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.settingsRepo,
            (LocalContext.current.applicationContext as EchoWaveApp).container.dnaRepository,
        ),
    ),
) {
    val displayName by vm.displayName.collectAsState()
    val greetingEnabled by vm.greetingEnabled.collectAsState()
    val musicPreferences by vm.musicPreferences.collectAsState()
    val dnaEnabled by vm.dnaEnabled.collectAsState()
    val isResettingDna by vm.isResettingDna.collectAsState()
    val dnaError by vm.dnaError.collectAsState()
    val uri = LocalUriHandler.current
    var showResetDnaConfirmation by remember { mutableStateOf(false) }
    var showDnaInfo by remember { mutableStateOf(false) }
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp))

        Surface(shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(horizontal = 14.dp)) {
                SettingsNavRow("Appearance", Icons.Default.Palette, onOpenAppearance)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingsNavRow("Playback", Icons.Default.GraphicEq, onOpenPlayback)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingsNavRow("Listening Summary", Icons.Default.BarChart, onOpenListeningSummary)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingsNavRow("About EchoWave", Icons.Default.Info, onOpenAbout)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingsNavRow("Send feedback or request a feature", Icons.Default.Feedback) {
                    uri.openUri("https://github.com/AyushDevHub/EchoWave/issues/new/choose")
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle("Your taste", Icons.Default.MusicNote)
        Surface(shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(
                    if (displayName.isNotBlank()) displayName else "No name set",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    if (musicPreferences.isNotBlank()) musicPreferences else "No taste picked yet",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
                TextButton(onClick = onEditTaste, modifier = Modifier.padding(top = 4.dp)) {
                    Text("Edit name & taste")
                }
            }
        }

        Spacer(Modifier.height(12.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).padding(end = 12.dp)) {
                        Text("Personalize recommendations", style = MaterialTheme.typography.bodyLarge)
                    }
                    Switch(checked = dnaEnabled, onCheckedChange = vm::setDnaEnabled)
                    IconButton(onClick = { showDnaInfo = true }) {
                        Icon(Icons.Default.Info, contentDescription = "About Music DNA",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                TextButton(
                    onClick = { showResetDnaConfirmation = true },
                    enabled = !isResettingDna,
                    modifier = Modifier.padding(top = 4.dp),
                ) {
                    Text(if (isResettingDna) "Clearing Music DNA…" else "Reset Music DNA")
                }
                if (dnaError != null) {
                    Text(
                        dnaError.orEmpty(),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle("A personal welcome", Icons.Default.Info)
        Surface(shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Show a hello on opening", Modifier.weight(1f),
                    style = MaterialTheme.typography.bodyLarge)
                Switch(checked = greetingEnabled, onCheckedChange = vm::setGreetingEnabled)
            }
        }
        Spacer(Modifier.height(28.dp))

        if (showResetDnaConfirmation) {
            AlertDialog(
                onDismissRequest = { showResetDnaConfirmation = false },
                title = { Text("Reset Music DNA?") },
                text = { Text("This permanently deletes the listening signals and taste profile stored on this device. It does not delete your playlists or favorites.") },
                confirmButton = {
                    TextButton(onClick = {
                        showResetDnaConfirmation = false
                        vm.resetDna()
                    }) { Text("Reset") }
                },
                dismissButton = {
                    TextButton(onClick = { showResetDnaConfirmation = false }) { Text("Cancel") }
                },
            )
        }
        if (showDnaInfo) {
            AlertDialog(
                onDismissRequest = { showDnaInfo = false },
                title = { Text("Music DNA") },
                text = { Text("Listening signals update recommendations on this device. Turn this off to pause collection. Reset it here to delete saved signals and your profile.") },
                confirmButton = { TextButton(onClick = { showDnaInfo = false }) { Text("Done") } },
            )
        }
    }
}

@Composable
fun AppearanceScreen(
    onBack: () -> Unit = {},
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.settingsRepo,
        ),
    ),
) {
    val themePreset by vm.themePreset.collectAsState()
    val fontChoice by vm.fontChoice.collectAsState()
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings")
            }
            Text("Appearance", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.height(10.dp))

        SettingsSectionTitle("Palette", Icons.Default.Palette)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                ThemePreset.entries.toList().chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(bottom = 8.dp)) {
                        row.forEach { option ->
                            val selected = themePreset == option
                            Surface(
                                onClick = { vm.setThemePreset(option) },
                                shape = RoundedCornerShape(16.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                                else MaterialTheme.colorScheme.surface,
                                border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 11.dp)) {
                                    Box(Modifier.size(18.dp).clip(CircleShape)
                                        .background(Color(option.primary.toInt())))
                                    Text(option.label, style = MaterialTheme.typography.labelMedium,
                                        modifier = Modifier.padding(start = 8.dp))
                                }
                            }
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        Spacer(Modifier.height(22.dp))
        SettingsSectionTitle("Font", Icons.Default.TextFields)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                FontChoice.entries.forEach { option ->
                    val selected = fontChoice == option
                    Surface(
                        onClick = { vm.setFontChoice(option) },
                        shape = RoundedCornerShape(16.dp),
                        color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                        else MaterialTheme.colorScheme.surface,
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 13.dp)) {
                            Text("Ag", style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(end = 12.dp),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant)
                            Column(Modifier.weight(1f)) {
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            }
                            if (selected) Text("ON", style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun PlaybackSettingsScreen(
    onBack: () -> Unit = {},
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.settingsRepo,
        ),
    ),
) {
    var showStyleInfo by remember { mutableStateOf(false) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings")
            }
            Text("Playback", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SettingInfoRow("Gapless playback", "", Icons.Default.Bolt)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingInfoRow("Audio quality", "", Icons.Default.GraphicEq)
            }
        }
        Spacer(Modifier.height(16.dp))
        Text("Player", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp))
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 6.dp, top = 8.dp, bottom = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Classic", style = MaterialTheme.typography.bodyLarge)
                    Text("Glow lyrics · sing along", style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showStyleInfo = true }) {
                    Icon(Icons.Default.Info, contentDescription = "About the classic player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        if (showStyleInfo) AlertDialog(
            onDismissRequest = { showStyleInfo = false },
            title = { Text("Classic player") },
            text = { Text("Artwork glow with synced, word-timed lyrics. Lyrics highlight each word as it plays when word timing is available.") },
            confirmButton = { TextButton(onClick = { showStyleInfo = false }) { Text("Done") } },
        )
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
fun AboutSettingsScreen(onBack: () -> Unit = {}) {
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(top = 12.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back to settings")
            }
            Text("About", style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(start = 4.dp))
        }
        Spacer(Modifier.height(8.dp))
        Surface(
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.13f)),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column {
                Box(
                    Modifier.fillMaxWidth().height(150.dp).background(
                        Brush.linearGradient(listOf(Color(0xFF321447), Color(0xFF17131D), Color(0xFF58306D))),
                    ),
                ) {
                    Box(
                        Modifier.align(Alignment.CenterStart).padding(start = 22.dp).size(72.dp)
                            .clip(RoundedCornerShape(22.dp)).background(Color.White.copy(alpha = 0.11f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        AppLogo(
                            size = 56.dp,
                            shape = RoundedCornerShape(16.dp),
                        )
                    }
                    Column(Modifier.align(Alignment.CenterStart).padding(start = 112.dp, end = 16.dp)) {
                        Text("EchoWave", style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(bottom = 12.dp))
                    AboutLink("Project on GitHub", Icons.AutoMirrored.Filled.OpenInNew) {
                        uri.openUri(SettingsRepository.GITHUB_URL)
                    }
                    AboutLink("GPL-3.0 license", Icons.Default.ChevronRight) {
                        uri.openUri("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                    AboutLink("Credits & acknowledgements", Icons.Default.ChevronRight) {
                        uri.openUri(SettingsRepository.CREDITS_URL)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
    }
}

@Composable
private fun SettingsNavRow(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    onClick: () -> Unit,
) {
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp))
            Text(title, Modifier.weight(1f).padding(start = 14.dp), style = MaterialTheme.typography.bodyLarge)
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(20.dp))
        }
}

@Composable
private fun SettingsSectionTitle(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(bottom = 10.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(19.dp))
        Text(title, style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(start = 9.dp))
    }
}

@Composable
private fun SettingInfoRow(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Row(Modifier.fillMaxWidth().padding(vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp))
        Column(Modifier.weight(1f).padding(start = 14.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            if (subtitle.isNotBlank()) {
                Text(subtitle, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Text("AUTO", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun AboutLink(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).clickable(onClick = onClick)
            .padding(vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp))
    }
}
