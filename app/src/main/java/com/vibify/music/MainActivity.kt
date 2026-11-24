package com.vibify.music

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vibify.music.ui.MainViewModel
import com.vibify.music.ui.components.MiniPlayer
import com.vibify.music.ui.components.QueueScreen
import com.vibify.music.ui.components.SongDetailsSheet
import com.vibify.music.ui.navigation.Screen
import com.vibify.music.ui.screens.KahaniScreen
import com.vibify.music.ui.screens.HomeScreen
import com.vibify.music.ui.screens.LibraryScreen
import com.vibify.music.ui.screens.NotificationsScreen
import com.vibify.music.ui.screens.NowPlayingScreen
import com.vibify.music.ui.screens.PlaylistDetailScreen
import com.vibify.music.ui.screens.PodcastShowScreen
import com.vibify.music.ui.screens.PodcastsScreen
import com.vibify.music.ui.screens.SearchScreen
import com.vibify.music.ui.screens.SettingsScreen
import com.vibify.music.ui.screens.StoryBookScreen
import com.vibify.music.ui.theme.LocalHasMiniPlayer
import com.vibify.music.ui.theme.LocalHideKeyboard
import com.vibify.music.ui.theme.LocalOpenTrackDetails
import com.vibify.music.ui.theme.SpotifyBlack
import com.vibify.music.ui.theme.SpotifyGreen
import com.vibify.music.ui.theme.SpotifySubtext
import com.vibify.music.ui.theme.SpotifyWhite
import com.vibify.music.ui.theme.VibifyTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }

        if (audioGranted) {
            viewModel.scanLocalSongs()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            VibifyTheme {
                MainAppContent(viewModel = viewModel, onRequestPermissions = { requestAppPermissions() })
            }
        }

        requestAppPermissions()
    }

    private fun requestAppPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            viewModel.scanLocalSongs()
        }
    }
}

@Composable
fun MainAppContent(
    viewModel: MainViewModel,
    onRequestPermissions: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val homeFeed by viewModel.homeFeed.collectAsState()
    val isHomeLoading by viewModel.isHomeLoading.collectAsState()
    val trendingVisibleCount by viewModel.trendingVisibleCount.collectAsState()
    val isLoadingMoreTrending by viewModel.isLoadingMoreTrending.collectAsState()
    val searchVisibleCount by viewModel.searchVisibleCount.collectAsState()
    val isLoadingMoreSearch by viewModel.isLoadingMoreSearch.collectAsState()
    val libraryVisibleCount by viewModel.libraryVisibleCount.collectAsState()
    val isLoadingMoreLibrary by viewModel.isLoadingMoreLibrary.collectAsState()
    val playlistVisibleCount by viewModel.playlistVisibleCount.collectAsState()
    val isLoadingMorePlaylist by viewModel.isLoadingMorePlaylist.collectAsState()
    val selectedPlaylist by viewModel.selectedPlaylist.collectAsState()
    val notifications by viewModel.notifications.collectAsState()
    val playerState by viewModel.playerState.collectAsState()
    val likedTrackIds by viewModel.likedTrackIds.collectAsState()
    val likedTracks by viewModel.likedTracks.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchSuggestions by viewModel.searchSuggestions.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()
    val searchPlaylists by viewModel.searchPlaylists.collectAsState()
    val popularSearches by viewModel.popularSearches.collectAsState()
    val browseCategories by viewModel.browseCategories.collectAsState()
    val isSearching by viewModel.isSearching.collectAsState()
    val podcastQuery by viewModel.podcastQuery.collectAsState()
    val podcastShows by viewModel.podcastShows.collectAsState()
    val podcastLoading by viewModel.podcastLoading.collectAsState()
    val selectedPodcast by viewModel.selectedPodcast.collectAsState()
    val podcastEpisodes by viewModel.podcastEpisodes.collectAsState()
    val podcastEpisodesLoading by viewModel.podcastEpisodesLoading.collectAsState()
    val storyQuery by viewModel.storyQuery.collectAsState()
    val storyBooks by viewModel.storyBooks.collectAsState()
    val storyLoading by viewModel.storyLoading.collectAsState()
    val selectedStory by viewModel.selectedStory.collectAsState()
    val storyChapters by viewModel.storyChapters.collectAsState()
    val storyChaptersLoading by viewModel.storyChaptersLoading.collectAsState()
    val isNowPlayingExpanded by viewModel.isNowPlayingExpanded.collectAsState()
    val trackDetails by viewModel.trackDetails.collectAsState()
    val error by viewModel.error.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val hideBottomBar = currentRoute == "settings" ||
        currentRoute == "notifications" ||
        currentRoute == "playlist" ||
        currentRoute == "podcastShow" ||
        currentRoute == "storyBook"

    LaunchedEffect(error) {
        val errorValue = error
        if (errorValue != null) {
            snackbarHostState.showSnackbar(
                errorValue,
                duration = androidx.compose.material3.SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    val navItems = listOf(
        Screen.Home,
        Screen.Search,
        Screen.Podcasts,
        Screen.Kahani,
        Screen.Library,
        Screen.Queue
    )

    BackHandler(enabled = isNowPlayingExpanded) {
        viewModel.setNowPlayingExpanded(false)
    }

    val showMiniPlayer = playerState.currentTrack != null &&
        currentRoute != "settings" &&
        currentRoute != "notifications" &&
        !isNowPlayingExpanded
    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current
    val hideKeyboard: () -> Unit = {
        focusManager.clearFocus(force = true)
        keyboardController?.hide()
        Unit
    }

    LaunchedEffect(playerState.currentTrack?.id, isNowPlayingExpanded) {
        if (playerState.currentTrack != null || isNowPlayingExpanded) {
            hideKeyboard()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalHasMiniPlayer provides showMiniPlayer,
            LocalHideKeyboard provides hideKeyboard,
            LocalOpenTrackDetails provides { track ->
                hideKeyboard()
                viewModel.showTrackDetails(track)
            }
        ) {
        Scaffold(
            containerColor = SpotifyBlack,
            bottomBar = {
                if (!hideBottomBar) {
                    NavigationBar(
                        containerColor = SpotifyBlack,
                        contentColor = SpotifyWhite,
                        tonalElevation = 0.dp
                    ) {
                        navItems.forEach { screen ->
                            val isSelected = currentRoute == screen.route
                            NavigationBarItem(
                                selected = isSelected,
                                onClick = {
                                    if (currentRoute != screen.route) {
                                        navController.navigate(screen.route) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                        contentDescription = screen.title
                                    )
                                },
                                label = {
                                    Text(
                                        text = screen.title,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        color = if (isSelected) SpotifyWhite else SpotifySubtext
                                    )
                                },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = SpotifyGreen,
                                    unselectedIconColor = SpotifySubtext,
                                    selectedTextColor = SpotifyWhite,
                                    indicatorColor = SpotifyGreen.copy(alpha = 0.16f)
                                )
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(SpotifyBlack)
            ) {
                SnackbarHost(hostState = snackbarHostState)

                NavHost(
                    navController = navController,
                    startDestination = Screen.Home.route,
                    modifier = Modifier.fillMaxSize()
                ) {
                    composable(Screen.Home.route) {
                        HomeScreen(
                            homeFeed = homeFeed,
                            isLoading = isHomeLoading,
                            trendingVisibleCount = trendingVisibleCount,
                            isLoadingMoreTrending = isLoadingMoreTrending,
                            playerState = playerState,
                            likedTrackIds = likedTrackIds,
                            notificationCount = notifications.size,
                            onTrackClick = { viewModel.playTrack(it) },
                            onLikeClick = { viewModel.toggleLike(it) },
                            onRefresh = { viewModel.refreshHome() },
                            onOpenNotifications = { navController.navigate("notifications") },
                            onOpenSettings = { navController.navigate("settings") },
                            onLoadMoreTrending = { viewModel.loadMoreTrending() },
                            onOpenPlaylist = { playlist ->
                                viewModel.openPlaylist(playlist)
                                navController.navigate("playlist")
                            }
                        )
                    }

                    composable("playlist") {
                        val playlist = selectedPlaylist
                        if (playlist == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            PlaylistDetailScreen(
                                playlist = playlist,
                                playerState = playerState,
                                likedTrackIds = likedTrackIds,
                                visibleCount = playlistVisibleCount,
                                isLoadingMore = isLoadingMorePlaylist,
                                onBack = {
                                    viewModel.clearSelectedPlaylist()
                                    navController.popBackStack()
                                },
                                onPlayAll = { viewModel.playPlaylist(playlist) },
                                onTrackClick = { _, index ->
                                    viewModel.playPlaylist(playlist, index)
                                },
                                onLikeClick = { viewModel.toggleLike(it) },
                                onLoadMore = { viewModel.loadMorePlaylist() }
                            )
                        }
                    }

                    composable(Screen.Podcasts.route) {
                        PodcastsScreen(
                            query = podcastQuery,
                            shows = podcastShows,
                            isLoading = podcastLoading,
                            onQueryChange = { viewModel.onPodcastQueryChange(it) },
                            onSearch = { viewModel.searchPodcasts(it) },
                            onOpenShow = { show ->
                                viewModel.openPodcast(show)
                                navController.navigate("podcastShow")
                            }
                        )
                    }

                    composable("podcastShow") {
                        val show = selectedPodcast
                        if (show == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            PodcastShowScreen(
                                show = show,
                                episodes = podcastEpisodes,
                                isLoading = podcastEpisodesLoading,
                                onBack = {
                                    viewModel.clearSelectedPodcast()
                                    navController.popBackStack()
                                },
                                onPlay = { index -> viewModel.playPodcastEpisode(index) }
                            )
                        }
                    }

                    composable(Screen.Kahani.route) {
                        KahaniScreen(
                            query = storyQuery,
                            books = storyBooks,
                            isLoading = storyLoading,
                            onQueryChange = { viewModel.onStoryQueryChange(it) },
                            onSearch = { viewModel.searchStories(it) },
                            onOpenBook = { book ->
                                viewModel.openStory(book)
                                navController.navigate("storyBook")
                            }
                        )
                    }

                    composable("storyBook") {
                        val book = selectedStory
                        if (book == null) {
                            LaunchedEffect(Unit) { navController.popBackStack() }
                        } else {
                            StoryBookScreen(
                                book = book,
                                chapters = storyChapters,
                                isLoading = storyChaptersLoading,
                                onBack = {
                                    viewModel.clearSelectedStory()
                                    navController.popBackStack()
                                },
                                onPlay = { index -> viewModel.playStoryChapter(index) }
                            )
                        }
                    }

                    composable(Screen.Search.route) {
                        LaunchedEffect(Unit) { viewModel.refreshSearchDiscovery() }
                        SearchScreen(
                            searchQuery = searchQuery,
                            suggestions = searchSuggestions,
                            searchResults = searchResults,
                            searchPlaylists = searchPlaylists,
                            visibleCount = searchVisibleCount,
                            isLoadingMore = isLoadingMoreSearch,
                            categories = browseCategories,
                            popularSearches = popularSearches,
                            playerState = playerState,
                            likedTrackIds = likedTrackIds,
                            isSearching = isSearching,
                            onQueryChange = { viewModel.onSearchQueryChange(it) },
                            onSelectSuggestion = { viewModel.onSelectSuggestion(it) },
                            onTrackClick = { viewModel.playTrack(it) },
                            onLikeClick = { viewModel.toggleLike(it) },
                            onOpenPlaylist = { playlist ->
                                viewModel.openPlaylist(playlist)
                                navController.navigate("playlist")
                            },
                            onLoadMore = { viewModel.loadMoreSearch() }
                        )
                    }

                    composable(Screen.Library.route) {
                        LibraryScreen(
                            likedTracks = likedTracks,
                            localTracks = homeFeed.localSongs,
                            playerState = playerState,
                            likedTrackIds = likedTrackIds,
                            visibleCount = libraryVisibleCount,
                            isLoadingMore = isLoadingMoreLibrary,
                            onTrackClick = { viewModel.playTrack(it) },
                            onLikeClick = { viewModel.toggleLike(it) },
                            onScanLocalSongs = onRequestPermissions,
                            onFilterChanged = { viewModel.resetLibraryPagination() },
                            onLoadMore = { total -> viewModel.loadMoreLibrary(total) }
                        )
                    }

                    composable(Screen.Queue.route) {
                        QueueScreen(
                            queue = playerState.queue,
                            currentQueueIndex = playerState.currentQueueIndex,
                            currentTrack = playerState.currentTrack,
                            isPlaying = playerState.isPlaying,
                            onTrackClick = { viewModel.playTrack(it) },
                            onRemoveFromQueue = { viewModel.removeFromQueue(it) },
                            onClearQueue = { viewModel.clearQueue() }
                        )
                    }

                    composable("settings") {
                        SettingsScreen(
                            likedCount = likedTracks.size,
                            localCount = homeFeed.localSongs.size,
                            onBack = { navController.popBackStack() },
                            onRefreshHome = {
                                viewModel.refreshHome()
                                navController.popBackStack()
                            },
                            onScanLocal = onRequestPermissions,
                            onClearLikes = { viewModel.clearLikedSongs() }
                        )
                    }

                    composable("notifications") {
                        NotificationsScreen(
                            notifications = notifications,
                            onBack = { navController.popBackStack() },
                            onClearAll = { viewModel.clearNotifications() }
                        )
                    }
                }

                if (showMiniPlayer) {
                    val currentTrack = playerState.currentTrack!!
                    val isLiked = likedTrackIds.contains(currentTrack.id)

                    MiniPlayer(
                        playerState = playerState,
                        isLiked = isLiked,
                        onMiniPlayerClick = { viewModel.setNowPlayingExpanded(true) },
                        onPlayPauseClick = { viewModel.togglePlayPause() },
                        onLikeClick = { viewModel.toggleLike(currentTrack) },
                        onNext = { viewModel.playNext() },
                        onPrevious = { viewModel.playPrevious() },
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 2.dp)
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = isNowPlayingExpanded && playerState.currentTrack != null,
            enter = slideInVertically(initialOffsetY = { it }),
            exit = slideOutVertically(targetOffsetY = { it })
        ) {
            val currentTrack = playerState.currentTrack ?: return@AnimatedVisibility
            val isLiked = likedTrackIds.contains(currentTrack.id)

            NowPlayingScreen(
                playerState = playerState,
                isLiked = isLiked,
                onCollapseClick = { viewModel.setNowPlayingExpanded(false) },
                onPlayPauseClick = { viewModel.togglePlayPause() },
                onNextClick = { viewModel.playNext() },
                onPreviousClick = { viewModel.playPrevious() },
                onSeekTo = { viewModel.seekTo(it) },
                onShuffleClick = { viewModel.toggleShuffle() },
                onRepeatClick = { viewModel.toggleRepeat() },
                onRewindClick = { viewModel.seekBy(-15_000) },
                onForwardClick = { viewModel.seekBy(15_000) },
                onSpeedClick = { viewModel.cyclePlaybackSpeed() },
                onLikeClick = { viewModel.toggleLike(currentTrack) },
                onQueueClick = {
                    viewModel.setNowPlayingExpanded(false)
                    navController.navigate(Screen.Queue.route) {
                        launchSingleTop = true
                    }
                },
                onShareClick = {
                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_SUBJECT, "Check out this song on Vibify")
                        putExtra(Intent.EXTRA_TEXT, "${currentTrack.title} by ${currentTrack.artist}")
                    }
                    context.startActivity(Intent.createChooser(shareIntent, "Share via"))
                },
                onMoreClick = {
                    viewModel.setNowPlayingExpanded(false)
                    navController.navigate("settings")
                }
            )
        }

        val details = trackDetails
        if (details != null) {
            SongDetailsSheet(
                track = details,
                isLiked = likedTrackIds.contains(details.id),
                onDismiss = { viewModel.dismissTrackDetails() },
                onPlay = {
                    viewModel.playTrack(details)
                    viewModel.dismissTrackDetails()
                },
                onLike = { viewModel.toggleLike(details) }
            )
        }
        } // CompositionLocalProvider
    }
}
