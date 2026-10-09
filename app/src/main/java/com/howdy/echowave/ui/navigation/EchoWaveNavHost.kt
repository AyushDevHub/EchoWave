package com.howdy.echowave.ui.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.howdy.echowave.ui.discover.AlbumDetailScreen
import com.howdy.echowave.ui.discover.DiscoverViewModel
import com.howdy.echowave.ui.home.HomeScreen
import com.howdy.echowave.ui.library.LibraryScreen
import com.howdy.echowave.ui.library.LibraryViewModel
import com.howdy.echowave.ui.onboarding.OnboardingScreen
import com.howdy.echowave.ui.player.MiniPlayer
import com.howdy.echowave.ui.player.LyricsViewModel
import com.howdy.echowave.ui.player.NowPlayingScreen
import com.howdy.echowave.ui.search.SearchScreen
import com.howdy.echowave.ui.settings.AboutSettingsScreen
import com.howdy.echowave.ui.settings.AppearanceScreen
import com.howdy.echowave.ui.settings.PlaybackSettingsScreen
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
    val onboardingDone by repo.settingsRepo.onboardingCompleted.collectAsState(null)
    val playerStyle by repo.settingsRepo.playerStyle.collectAsState(com.howdy.echowave.data.local.PlayerStyle.APPLE)
    val libVm: LibraryViewModel = viewModel(factory = LibraryViewModel.Factory(repo.libraryRepo))
    val favoriteIds by libVm.favoriteIds.collectAsState()
    val history by libVm.history.collectAsState()
    val playlists by libVm.playlists.collectAsState()
    val lyricsVm: LyricsViewModel = viewModel(factory = LyricsViewModel.Factory(repo.lyricsRepo))
    val lyricsUi by lyricsVm.ui.collectAsState()
    val discoverVm: DiscoverViewModel = viewModel(factory = DiscoverViewModel.Factory(repo.discoveryRepo))
    val discoverUi by discoverVm.ui.collectAsState()
    var selectedAlbum by remember { mutableStateOf<com.howdy.echowave.domain.model.SearchItem.Album?>(null) }
    var albumTracks by remember { mutableStateOf(emptyList<com.howdy.echowave.domain.model.Track>()) }
    var albumLoading by remember { mutableStateOf(false) }
    var albumError by remember { mutableStateOf<String?>(null) }
    var albumRetry by rememberSaveable { mutableStateOf(0) }

    LaunchedEffect(entry?.destination?.route, selectedAlbum?.id, albumRetry) {
        val album = selectedAlbum
        if (entry?.destination?.route == Routes.ALBUM && album != null) {
            albumLoading = true
            albumError = null
            albumTracks = emptyList()
            when (val result = repo.discoveryRepo.albumTracks(album.id, album.browseParams)) {
                is com.howdy.echowave.core.common.AppResult.Ok -> {
                    albumTracks = result.value
                }
                is com.howdy.echowave.core.common.AppResult.Err -> albumError = result.message
            }
            albumLoading = false
        }
    }

    if (onboardingDone == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            androidx.compose.material3.CircularProgressIndicator()
        }
        return
    }

    Scaffold(
        bottomBar = {
            val route = entry?.destination?.route
            if (route != Routes.NOW_PLAYING && route != Routes.ONBOARDING && route != Routes.ONBOARDING_EDIT) Column {
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
                            onClick = {
                                nav.navigate(route) {
                                    launchSingleTop = true
                                    restoreState = true
                                    popUpTo(nav.graph.startDestinationId) { saveState = true }
                                }
                            },
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
            startDestination = if (onboardingDone == true) Routes.HOME else Routes.ONBOARDING,
            modifier = Modifier.padding(padding),
            enterTransition = {
                fadeIn(tween(280)) + slideInHorizontally(tween(280)) { it / 20 } + scaleIn(tween(280), initialScale = 0.98f)
            },
            exitTransition = { fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.99f) },
            popEnterTransition = {
                fadeIn(tween(280)) + slideInHorizontally(tween(280)) { -it / 20 } + scaleIn(tween(280), initialScale = 0.98f)
            },
            popExitTransition = { fadeOut(tween(160)) + scaleOut(tween(160), targetScale = 0.99f) },
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onComplete = {
                        nav.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    },
                )
            }
            composable(Routes.ONBOARDING_EDIT) {
                OnboardingScreen(
                    editMode = true,
                    initialName = displayName,
                    onBack = { nav.popBackStack() },
                    onComplete = { nav.popBackStack() },
                )
            }
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
                            is com.howdy.echowave.core.common.AppResult.Ok -> result.value.songs
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
                    displayName = displayName,
                    greetingEnabled = greetingEnabled,
                    personalizedTracks = tasteTracks,
                    preferenceLoading = tasteLoading,
                    onPlay = { tracks, i ->
                        scope.launch { controller.play(tracks, i) }
                    },
                    onOpenPlayer = { nav.navigate(Routes.NOW_PLAYING) },
                    onSeeAllCharts = { nav.navigate(Routes.CHARTS) },
                )
            }
            composable(Routes.SEARCH) {
                SearchScreen(
                    browseTracks = discoverUi.charts + history,
                    onPlay = { tracks, i ->
                        android.util.Log.d("EchoWavePlay", "tap trackId=${tracks.getOrNull(i)?.id} index=$i")
                        val list = tracks.ifEmpty { emptyList() }
                        if (list.isNotEmpty() && i in list.indices) {
                            scope.launch { controller.play(list, i) }
                        }
                    },
                    onPlayRadio = { track ->
                        scope.launch { controller.playRadio(track) }
                    },
                    favoriteIds = favoriteIds,
                    onToggleFavorite = libVm::toggle,
                    onOpenAlbum = { album ->
                        selectedAlbum = album
                        albumRetry++
                        nav.navigate(Routes.ALBUM)
                    },
                )
            }
            composable(Routes.LIBRARY) {
                LibraryScreen(onPlay = { tracks, i ->
                    scope.launch { controller.play(tracks, i) }
                })
            }
            composable(Routes.ALBUM) {
                val album = selectedAlbum
                if (album != null) {
                    AlbumDetailScreen(
                        title = album.title,
                        artist = album.artist,
                        artworkUrl = album.artworkUrl,
                        tracks = albumTracks,
                        loading = albumLoading,
                        error = albumError,
                        onBack = { nav.popBackStack() },
                        onRetry = { albumRetry++ },
                        onPlay = { tracks, index -> scope.launch { controller.play(tracks, index) } },
                    )
                } else {
                    // Process death / rotation cleared shared album state.
                    // Show recoverable empty state instead of blank screen.
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = androidx.compose.foundation.layout.Arrangement.Center,
                    ) {
                        Text("Album unavailable", style = androidx.compose.material3.MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.material3.TextButton(onClick = { nav.popBackStack() }) {
                            Text("Back")
                        }
                    }
                }
            }
            composable(Routes.CHARTS) {
                ChartsScreen(
                    tracks = discoverUi.charts,
                    loading = discoverUi.loadingCharts,
                    error = discoverUi.error,
                    onRetry = { discoverVm.refresh() },
                    onPlay = { tracks, i ->
                        scope.launch { controller.play(tracks, i) }
                    },
                )
            }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    onOpenAppearance = { nav.navigate(Routes.SETTINGS_APPEARANCE) },
                    onOpenPlayback = { nav.navigate(Routes.SETTINGS_PLAYBACK) },
                    onOpenAbout = { nav.navigate(Routes.SETTINGS_ABOUT) },
                    onEditTaste = { nav.navigate(Routes.ONBOARDING_EDIT) },
                )
            }
            composable(Routes.SETTINGS_APPEARANCE) { AppearanceScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS_PLAYBACK) { PlaybackSettingsScreen(onBack = { nav.popBackStack() }) }
            composable(Routes.SETTINGS_ABOUT) { AboutSettingsScreen(onBack = { nav.popBackStack() }) }
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
                    onVolume = controller::setVolume,
                    playerStyle = playerStyle,
                    onRetry = {
                        val s = controller.state.value
                        if (s.queue.isNotEmpty() && s.queueIndex in s.queue.indices) {
                            scope.launch { controller.play(s.queue, s.queueIndex) }
                        }
                    },
                    isFavorite = current?.id in favoriteIds,
                    onToggleFavorite = { current?.let { libVm.toggle(it) } },
                    onPlayQueueAt = { i ->
                        val s = controller.state.value
                        scope.launch { controller.play(s.queue, i) }
                    },
                    playlists = playlists,
                    onCreateAndAdd = { name ->
                        if (name.isBlank()) return@NowPlayingScreen
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
