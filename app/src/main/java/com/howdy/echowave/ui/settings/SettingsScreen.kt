package com.howdy.echowave.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.BuildConfig
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.data.local.SettingsRepository
import com.howdy.echowave.data.local.ThemePreset

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.settingsRepo,
        ),
    ),
) {
    val themePreset by vm.themePreset.collectAsState()
    val displayName by vm.displayName.collectAsState()
    val greetingEnabled by vm.greetingEnabled.collectAsState()
    val musicPreferences by vm.musicPreferences.collectAsState()
    val uri = LocalUriHandler.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp),
    ) {
        Text("YOUR LISTENING SPACE", modifier = Modifier.padding(top = 20.dp),
            style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text("Make it yours.", style = MaterialTheme.typography.headlineLarge,
            modifier = Modifier.padding(top = 4.dp))
        Text("A few little details for a better listening experience.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 22.dp))

        SettingsSectionTitle("Appearance", Icons.Default.Palette)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(16.dp)) {
                Text("Choose your palette", style = MaterialTheme.typography.titleMedium)
                Text("A set of colors for every listening mood.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp))
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

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle("Your listening taste", Icons.Default.MusicNote)
        Surface(shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text("Genres, languages, artists", style = MaterialTheme.typography.titleMedium)
                Text("Use a few words such as Hindi, Punjabi, indie or Bhojpuri to shape browse suggestions.",
                    style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 3.dp, bottom = 8.dp))
                OutlinedTextField(value = musicPreferences, onValueChange = vm::setMusicPreferences,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    placeholder = { Text("Hindi, indie, soft rock…") },
                    shape = RoundedCornerShape(16.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle("A personal welcome", Icons.Default.Info)
        Surface(shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                OutlinedTextField(value = displayName, onValueChange = vm::setDisplayName,
                    modifier = Modifier.fillMaxWidth(), singleLine = true,
                    label = { Text("Your name") }, placeholder = { Text("What should EchoWave call you?") },
                    shape = RoundedCornerShape(16.dp))
                Row(Modifier.fillMaxWidth().padding(top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Show a hello on opening", style = MaterialTheme.typography.bodyLarge)
                        Text("Turn this off whenever you like.", style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = greetingEnabled, onCheckedChange = vm::setGreetingEnabled)
                }
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle("Playback", Icons.Default.GraphicEq)
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 4.dp)) {
                SettingInfoRow("Gapless playback", "Seamless when the stream supports it", Icons.Default.Bolt)
                androidx.compose.material3.HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                SettingInfoRow("Audio quality", "Best available from the source", Icons.Default.GraphicEq)
            }
        }

        Spacer(Modifier.height(24.dp))
        SettingsSectionTitle("About EchoWave", Icons.Default.Info)
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
                        Icon(Icons.Default.MusicNote, contentDescription = null,
                            tint = Color(0xFFE6B4FF), modifier = Modifier.size(38.dp))
                    }
                    Column(Modifier.align(Alignment.CenterStart).padding(start = 112.dp, end = 16.dp)) {
                        Text("EchoWave", style = MaterialTheme.typography.headlineSmall,
                            color = Color.White, fontWeight = FontWeight.Bold)
                        Text("Your music, in full color.", style = MaterialTheme.typography.bodyMedium,
                            color = Color.White.copy(alpha = 0.72f))
                    }
                }
                Column(Modifier.padding(horizontal = 18.dp, vertical = 14.dp)) {
                    Text("Version ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                        style = MaterialTheme.typography.titleMedium)
                    Text("Made for the moments between everything.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 3.dp, bottom = 12.dp))
                    AboutLink("Project on GitHub", Icons.Default.OpenInNew) {
                        uri.openUri(SettingsRepository.GITHUB_URL)
                    }
                    AboutLink("GPL-3.0 license", Icons.Default.ChevronRight) {
                        uri.openUri("https://www.gnu.org/licenses/gpl-3.0.en.html")
                    }
                    AboutLink("Credits & acknowledgements", Icons.Default.ChevronRight) {
                        uri.openUri(SettingsRepository.GITHUB_URL)
                    }
                }
            }
        }
        Spacer(Modifier.height(28.dp))
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
            Text(subtitle, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
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
