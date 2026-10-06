package com.howdy.echowave

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.howdy.echowave.playback.PlaybackSessionConnector
import com.howdy.echowave.ui.navigation.EchoWaveNavHost
import com.howdy.echowave.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private var connector: PlaybackSessionConnector? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as EchoWaveApp).container
        connector = PlaybackSessionConnector(this, container.playback)
        setContent {
            MyApplicationTheme {
                EchoWaveNavHost(controller = container.playback)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        connector?.connect()
    }

    override fun onStop() {
        connector?.release()
        super.onStop()
    }
}
