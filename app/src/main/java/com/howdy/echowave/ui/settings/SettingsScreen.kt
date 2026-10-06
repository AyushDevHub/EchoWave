package com.howdy.echowave.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
fun SettingsScreen() {
    Column {
        Text("Settings")
        Text("Appearance: Dark / Light / System")
        Text("Playback: gapless + quality (when source allows)")
        Text("About: version, GitHub, licenses, credits")
    }
}
