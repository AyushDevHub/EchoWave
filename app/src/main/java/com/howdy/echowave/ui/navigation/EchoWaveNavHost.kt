package com.howdy.echowave.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import com.howdy.echowave.ui.discover.ChartsScreen
import com.howdy.echowave.ui.discover.DiscoverViewModel
import com.howdy.echowave.ui.discover.MoodsScreen
import com.howdy.echowave.ui.discover.NewReleasesScreen
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
    val displayName by repo.settingsRepo.displayName.collectAsState("")
    val greetingEnabled by repo.settingsRepo.greetingEnabled.collectAsState(true)
    val musicPreferences by repo.settingsRepo.musicPreferences.collectAsState("")
    val libVm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(repo.libraryRepo))
    val favoriteIds by libVm.favoriteIds.collectAsState()
    val history by libVm.history.collectAsState()
    val playlists by libVm.playlists.collectAsState()
    val lyricsVm: LyricsViewModel = viewModel(factory = LyricsViewModel.Factory(repo.lyricsRepo))
    val lyricsUi by lyricsVm.ui.collectAsState()
    val discoverVm: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory(repo.discoveryRepo))
    val discoverUi by discoverVm.ui.collectAsState()

    Scaffold(
        bottomBar = {
            if (entry?.destination?.route != Routes.NOW_PLAYING) Column {
                MiniPlayer(
                    state = state,
                    onToggle = controller::toggle,
                    onPrevious = controller::previous,
                    onNext = controller::next,
                    onOpen = { nav.navigate(Routes.NOW_PLAYING) },
                )
                Box(Modifier.fillMaxWidth().navigationBarsPadding()) {
                NavigationBar(
                    modifier = Modifier.widthIn(max = 390.dp).align(Alignment.Center)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(28.dp)),
                    windowInsets = WindowInsets(0.dp),
                    containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface.copy(alpha = 0.82f),
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
            }
        },
    ) { padding ->
        NavHost(
            nav,
            startDestination = Routes.HOME,
            modifier = Modifier.padding(padding),
            enterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 18 } },
            exitTransition = { fadeOut(tween(140)) },
            popEnterTransition = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 18 } },
            popExitTransition = { fadeOut(tween(140)) },
        ) {
            composable(Routes.HOME) {
                var tasteTracks by remember { mutableStateOf(emptyList<com.howdy.echowave.domain.model.Track>()) }
                var tasteLoading by remember { mutableStateOf(false) }
                LaunchedEffect(musicPreferences) {
                    val query = musicPreferences.trim()
                    if (query.isBlank()) {
                        tasteTracks = emptyList()
                        tasteLoading = false
                    } else {
                        tasteLoading = true
                        tasteTracks = when (val result = repo.musicRepo.search(query)) {
                            is com.howdy.echowave.core.common.AppResult.Ok -> result.value
                            is com.howdy.echowave.core.common.AppResult.Err -> emptyList()
                        }
                        tasteLoading = false
                    }
                }
                LaunchedEffect(Unit) {
                    libVm.refresh()
                    discoverVm.refresh()
                }
                val favs by libVm.favorites.collectAsState()
                HomeScreen(
                    recent = history,
                    favorites = favs,
                    current = state.currentTrack,
                    queue = state.queue,
                    queueIndex = state.queueIndex,
                    charts = discoverUi.charts,
                    albums = discoverUi.albums,
                    moods = discoverUi.moods,
                    displayName = displayName,
                    greetingEnabled = greetingEnabled,
                    personalizedTracks = tasteTracks,
                    preferenceLoading = tasteLoading,
                    onPlay = { tracks, i ->
                        scope.launch { controller.play(tracks, i) }
                    },
                    onOpenPlayer = { nav.navigate(Routes.NOW_PLAYING) },
                    onSeeAllCharts = { nav.navigate(Routes.CHARTS) },
                    onSeeAllAlbums = { nav.navigate(Routes.NEW_RELEASES) },
                    onSeeAllMoods = { nav.navigate(Routes.MOODS) },
                    onPlayAlbum = { album ->
                        scope.launch {
                            when (val r = repo.discoveryRepo.albumTracks(album.id)) {
                                is com.howdy.echowave.core.common.AppResult.Ok ->
                                    if (r.value.isNotEmpty()) controller.play(r.value, 0)
                                else -> Unit
                            }
                        }
                    },
                    onPlayMood = { mood ->
                        scope.launch {
                            when (val r = repo.discoveryRepo.moodTracks(mood)) {
                                is com.howdy.echowave.core.common.AppResult.Ok ->
                                    if (r.value.isNotEmpty()) controller.play(r.value, 0)
                                else -> Unit
                            }
                        }
                    },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    browseTracks = discoverUi.charts + history,
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
            composable(Routes.CHARTS) {
                ChartsScreen(
                    tracks = discoverUi.charts,
                    loading = discoverUi.loadingCharts,
                    onPlay = { tracks, i ->
                        scope.launch { controller.play(tracks, i) }
                    },
                )
            }
            composable(Routes.NEW_RELEASES) {
                NewReleasesScreen(
                    albums = discoverUi.albums,
                    loading = discoverUi.loadingAlbums,
                    onOpenAlbum = { album ->
                        scope.launch {
                            when (val r = repo.discoveryRepo.albumTracks(album.id)) {
                                is com.howdy.echowave.core.common.AppResult.Ok ->
                                    if (r.value.isNotEmpty()) controller.play(r.value, 0)
                                else -> Unit
                            }
                        }
                    },
                )
            }
            composable(Routes.MOODS) {
                MoodsScreen(
                    moods = discoverUi.moods,
                    loading = discoverUi.loadingMoods,
                    onOpenMood = { mood ->
                        scope.launch {
                            when (val r = repo.discoveryRepo.moodTracks(mood)) {
                                is com.howdy.echowave.core.common.AppResult.Ok ->
                                    if (r.value.isNotEmpty()) controller.play(r.value, 0)
                                else -> Unit
                            }
                        }
                    },
                )
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
                    onBack = { nav.popBackStack() },
                )
            }
        }
    }
}
