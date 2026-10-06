package com.howdy.echowave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.howdy.echowave.data.local.Appearance
import com.howdy.echowave.ui.navigation.EchoWaveNavHost
import com.howdy.echowave.ui.theme.MyApplicationTheme

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
            val appearance by container.settingsRepo.appearance.collectAsState(Appearance.SYSTEM)
            val systemDark = isSystemInDarkTheme()
            val dark = when (appearance) {
                Appearance.DARK -> true
                Appearance.LIGHT -> false
                Appearance.SYSTEM -> systemDark
            }
            MyApplicationTheme(darkTheme = dark) {
                EchoWaveNavHost(controller = container.playback)
            }
        }
    }
}
