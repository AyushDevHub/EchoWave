package com.howdy.echowave.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.howdy.echowave.playback.PlaybackController
import com.howdy.echowave.ui.home.HomeScreen
import com.howdy.echowave.ui.library.LibraryScreen
import com.howdy.echowave.ui.player.MiniPlayer
import com.howdy.echowave.ui.player.NowPlayingScreen
import com.howdy.echowave.ui.search.SearchScreen
import com.howdy.echowave.ui.settings.SettingsScreen
import kotlinx.coroutines.launch

@Composable
fun EchoWaveNavHost(controller: PlaybackController) {
    val nav = rememberNavController()
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    val entry by nav.currentBackStackEntryAsState()

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(state, onToggle = controller::toggle, onOpen = { nav.navigate(Routes.NOW_PLAYING) })
                NavigationBar {
                    val items = listOf(
                        Triple(Routes.HOME, "Home", Icons.Default.Home),
                        Triple(Routes.SEARCH, "Search", Icons.Default.Search),
                        Triple(Routes.LIBRARY, "Library", Icons.Default.LibraryMusic),
                        Triple(Routes.SETTINGS, "Settings", Icons.Default.Settings),
                    )
                    items.forEach { (route, label, icon) ->
                        NavigationBarItem(
                            selected = entry?.destination?.route == route,
                            onClick = { nav.navigate(route) { launchSingleTop = true } },
                            icon = { Icon(icon, label) },
                            label = { Text(label) },
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, Modifier.padding(padding)) {
            composable(Routes.HOME) {
                HomeScreen(recent = state.queue) { track ->
                    scope.launch { controller.play(listOf(track), 0) }
                }
            }
            composable(Routes.SEARCH) {
                SearchScreen(onPlay = { tracks, i ->
                    android.util.Log.d("EchoWavePlay", "tap trackId=${tracks.getOrNull(i)?.id} index=$i")
                    scope.launch { controller.play(tracks, i) }
                })
            }
            composable(Routes.LIBRARY) { LibraryScreen() }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.NOW_PLAYING) {
                NowPlayingScreen(
                    state = state,
                    onToggle = controller::toggle,
                    onNext = controller::next,
                    onPrev = controller::previous,
                    onSeek = controller::seekTo,
                    onShuffle = controller::setShuffle,
                    onRepeat = controller::setRepeat,
                    onRetry = {
                        val s = controller.state.value
                        scope.launch { controller.play(s.queue, s.queueIndex) }
                    },
                )
            }
        }
    }
}
