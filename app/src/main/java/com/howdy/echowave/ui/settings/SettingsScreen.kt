package com.howdy.echowave.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.BuildConfig
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.data.local.SettingsRepository
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.settingsRepo,
        ),
    ),
) {
    val appearance by vm.appearance.collectAsState()
    val uri = LocalUriHandler.current
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text(
            "Settings",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
        )
        SectionLabel("Appearance")
        for (option in Appearance.entries) {
            Row(
                Modifier.fillMaxWidth().clickable { vm.setAppearance(option) }
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = appearance == option, onClick = { vm.setAppearance(option) })
                Text(
                    option.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionLabel("Playback")
        SettingRow(
            title = "Gapless playback",
            subtitle = "On where the stream allows it. No toggle in MVP.",
        )
        SettingRow(
            title = "Audio quality",
            subtitle = "Best available from the source. No toggle in MVP.",
        )
        HorizontalDivider(Modifier.padding(vertical = 8.dp))
        SectionLabel("About")
        SettingRow(title = "Version", subtitle = "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Row(
            Modifier.fillMaxWidth().clickable { uri.openUri(SettingsRepository.GITHUB_URL) }
                .padding(vertical = 10.dp),
        ) {
            Column {
                Text("GitHub", style = MaterialTheme.typography.bodyLarge)
                Text(
                    SettingsRepository.GITHUB_URL,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        SettingRow(title = "License", subtitle = "GPL-3.0 — see LICENSE in the repo.")
        SettingRow(
            title = "Credits",
            subtitle = "Echo-Music · InnerTubeX · NewPipe lineage · Spotube · LastWave. Full list in CREDITS.md.",
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text.uppercase(),
        style = MaterialTheme.typography.labelLarge.copy(
            letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp),
        ),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingRow(title: String, subtitle: String) {
    Column(Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Text(title, style = MaterialTheme.typography.bodyLarge)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
