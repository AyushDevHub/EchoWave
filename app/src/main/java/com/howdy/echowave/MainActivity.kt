package com.howdy.echowave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.howdy.echowave.data.local.FontChoice
import com.howdy.echowave.data.local.ThemePreset
import com.howdy.echowave.ui.navigation.EchoWaveNavHost
import com.howdy.echowave.ui.theme.EchoWaveTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as EchoWaveApp).container
        // App-scoped connection: survives backgrounding so notification,
        // lock-screen and end-of-track advance always reach the player.
        // Releasing in onStop is what silently broke background control.
        container.sessionConnector.connect()
        setContent {
            val themePreset by container.settingsRepo.themePreset.collectAsState(ThemePreset.PURPLE)
            val fontChoice by container.settingsRepo.fontChoice.collectAsState(FontChoice.SYSTEM)
            EchoWaveTheme(preset = themePreset, fontChoice = fontChoice) {
                EchoWaveNavHost(controller = container.playback)
            }
        }
    }
}
