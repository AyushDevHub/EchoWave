package com.howdy.echowave.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.howdy.echowave.BuildConfig
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.data.local.SettingsRepository

@Composable
fun SettingsScreen(
    vm: SettingsViewModel = viewModel(
        factory = SettingsViewModel.Factory(
            (LocalContext.current.applicationContext as EchoWaveApp).container.settingsRepo,
        ),
    ),
) {
    val appearance by vm.appearance.collectAsState()
    Column(Modifier.fillMaxSize().padding(16.dp)) {
        Text("Settings")
        Text("Appearance")
        for (option in Appearance.entries) {
            Row(
                Modifier.fillMaxWidth().clickable { vm.setAppearance(option) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = appearance == option, onClick = { vm.setAppearance(option) })
                Text(option.name.lowercase().replaceFirstChar { it.uppercase() })
            }
        }
        Text("Playback")
        Text("Gapless and quality follow the source; no toggles in MVP.")
        Text("About")
        Text("EchoWave ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
        Text(SettingsRepository.GITHUB_URL)
        Text("GPL-3.0 — see LICENSE. Credits: Echo-Music, InnerTubeX, NewPipe lineage, Spotube, LastWave. Full list in CREDITS.md.")
    }
}
