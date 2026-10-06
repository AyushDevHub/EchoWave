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
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.howdy.echowave.EchoWaveApp
import com.howdy.echowave.playback.PlaybackController
import com.howdy.echowave.ui.home.HomeScreen
import com.howdy.echowave.ui.library.LibraryScreen
import com.howdy.echowave.ui.library.LibraryViewModel
import com.howdy.echowave.ui.player.MiniPlayer
import com.howdy.echowave.ui.player.LyricsViewModel
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
    val repo = (LocalContext.current.applicationContext as EchoWaveApp).container
    val libVm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(repo.libraryRepo))
    val favoriteIds by libVm.favoriteIds.collectAsState()
    val history by libVm.history.collectAsState()
    val playlists by libVm.playlists.collectAsState()
    val lyricsVm: LyricsViewModel = viewModel(factory = LyricsViewModel.Factory(repo.lyricsRepo))
    val lyricsUi by lyricsVm.ui.collectAsState()

    Scaffold(
        bottomBar = {
            Column {
                MiniPlayer(
                    state = state,
                    onToggle = controller::toggle,
                    onPrevious = controller::previous,
                    onNext = controller::next,
                    onOpen = { nav.navigate(Routes.NOW_PLAYING) },
                )
                NavigationBar(
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
                    contentColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                    tonalElevation = 0.dp,
                ) {
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
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.primary,
                                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                unselectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                                unselectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurfaceVariant,
                            ),
                        )
                    }
                }
            }
        },
    ) { padding ->
        NavHost(nav, startDestination = Routes.HOME, Modifier.padding(padding)) {
            composable(Routes.HOME) {
                LaunchedEffect(Unit) { libVm.refresh() }
                val favs by libVm.favorites.collectAsState()
                HomeScreen(
                    recent = history,
                    favorites = favs,
                    current = state.currentTrack,
                    onPlay = { tracks, i ->
                        scope.launch { controller.play(tracks, i) }
                    },
                    onOpenPlayer = { nav.navigate(Routes.NOW_PLAYING) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    onPlay = { tracks, i ->
                        android.util.Log.d("EchoWavePlay", "tap trackId=${tracks.getOrNull(i)?.id} index=$i")
                        scope.launch { controller.play(tracks, i) }
                    },
                    favoriteIds = favoriteIds,
                    onToggleFavorite = libVm::toggle,
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(onPlay = { tracks, i ->
                    scope.launch { controller.play(tracks, i) }
                })
            }
            composable(Routes.SETTINGS) { SettingsScreen() }
            composable(Routes.NOW_PLAYING) {
                val current = state.currentTrack
                LaunchedEffect(current?.id) { lyricsVm.load(current) }
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
                    isFavorite = current?.id in favoriteIds,
                    onToggleFavorite = { current?.let { libVm.toggle(it) } },
                    onPlayQueueAt = { i ->
                        val s = controller.state.value
                        scope.launch { controller.play(s.queue, i) }
                    },
                    playlists = playlists,
                    onCreateAndAdd = { name ->
                        val t = current ?: return@NowPlayingScreen
                        scope.launch {
                            runCatching { repo.libraryRepo.createPlaylist(name) }
                                .onSuccess { id -> repo.libraryRepo.addToPlaylist(id, t) }
                        }
                    },
                    onAddToPlaylist = { id ->
                        val t = current ?: return@NowPlayingScreen
                        scope.launch { repo.libraryRepo.addToPlaylist(id, t) }
                    },
                    lyrics = lyricsUi,
                    onRetryLyrics = { lyricsVm.load(current, forceRefresh = true) },
                )
            }
        }
    }
}
