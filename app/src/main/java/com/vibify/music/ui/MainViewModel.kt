package com.vibify.music.ui

import android.app.Application
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vibify.music.data.local.PlaybackSettingsStore
import com.vibify.music.data.local.PodcastProgressStore
import com.vibify.music.data.model.AutoPlaylist
import com.vibify.music.data.model.Category
import com.vibify.music.data.model.HomeFeed
import com.vibify.music.data.model.ListeningAction
import com.vibify.music.data.model.PodcastEpisode
import com.vibify.music.data.model.PodcastShow
import com.vibify.music.data.model.StoryBook
import com.vibify.music.data.model.StoryChapter
import com.vibify.music.data.model.Track
import com.vibify.music.data.network.PodcastEngine
import com.vibify.music.data.network.StoryEngine
import com.vibify.music.data.network.toTrack
import com.vibify.music.data.repository.MusicRepository
import com.vibify.music.player.PlayerState
import com.vibify.music.player.VibifyAudioPlayer
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class AppNotification(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
) {
    val timeLabel: String
        get() = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(timestamp))
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    companion object {
        const val PAGE_SIZE = 10
    }

    private val appContext = application.applicationContext
    private val repository = MusicRepository(appContext)
    private val audioPlayer = VibifyAudioPlayer.getInstance(appContext)
    private val podcastEngine = PodcastEngine()
    private val storyEngine = StoryEngine()
    private val podcastProgress = PodcastProgressStore(appContext)
    private val playbackSettings = PlaybackSettingsStore(appContext)
    private var podcastSearchJob: Job? = null
    private var podcastFeedJob: Job? = null
    private var storySearchJob: Job? = null
    private var storyChapterJob: Job? = null
    private var lastProgressSave = 0L
    private val connectivityManager = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val playerState: StateFlow<PlayerState> = audioPlayer.playerState
    val likedTrackIds: StateFlow<Set<String>> = repository.likedTrackIds

    private val _browseCategories = MutableStateFlow(repository.browseCategories())
    val browseCategories: StateFlow<List<Category>> = _browseCategories.asStateFlow()

    private val _popularSearches = MutableStateFlow(repository.popularSearchTags())
    val popularSearches: StateFlow<List<String>> = _popularSearches.asStateFlow()

    private val _searchPlaylists = MutableStateFlow<List<AutoPlaylist>>(emptyList())
    val searchPlaylists: StateFlow<List<AutoPlaylist>> = _searchPlaylists.asStateFlow()

    private val _hasAudioPermission = MutableStateFlow(false)
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    private val _localTracksFound = MutableStateFlow(false)
    val localTracksFound: StateFlow<Boolean> = _localTracksFound.asStateFlow()

    private val _isHomeLoading = MutableStateFlow(false)
    val isHomeLoading: StateFlow<Boolean> = _isHomeLoading.asStateFlow()

    private val _trendingVisibleCount = MutableStateFlow(PAGE_SIZE)
    val trendingVisibleCount: StateFlow<Int> = _trendingVisibleCount.asStateFlow()

    private val _isLoadingMoreTrending = MutableStateFlow(false)
    val isLoadingMoreTrending: StateFlow<Boolean> = _isLoadingMoreTrending.asStateFlow()

    private val _searchVisibleCount = MutableStateFlow(PAGE_SIZE)
    val searchVisibleCount: StateFlow<Int> = _searchVisibleCount.asStateFlow()

    private val _isLoadingMoreSearch = MutableStateFlow(false)
    val isLoadingMoreSearch: StateFlow<Boolean> = _isLoadingMoreSearch.asStateFlow()

    private val _libraryVisibleCount = MutableStateFlow(PAGE_SIZE)
    val libraryVisibleCount: StateFlow<Int> = _libraryVisibleCount.asStateFlow()

    private val _isLoadingMoreLibrary = MutableStateFlow(false)
    val isLoadingMoreLibrary: StateFlow<Boolean> = _isLoadingMoreLibrary.asStateFlow()

    private val _playlistVisibleCount = MutableStateFlow(PAGE_SIZE)
    val playlistVisibleCount: StateFlow<Int> = _playlistVisibleCount.asStateFlow()

    private val _isLoadingMorePlaylist = MutableStateFlow(false)
    val isLoadingMorePlaylist: StateFlow<Boolean> = _isLoadingMorePlaylist.asStateFlow()

    private val _notifications = MutableStateFlow<List<AppNotification>>(emptyList())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    fun checkAudioPermission(): Boolean {
        val granted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_MEDIA_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(appContext, android.Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
        }
        _hasAudioPermission.value = granted
        return granted
    }

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    private val _searchResults = MutableStateFlow<List<Track>>(emptyList())
    val searchResults: StateFlow<List<Track>> = _searchResults.asStateFlow()

    private val _isSearching = MutableStateFlow(false)
    val isSearching: StateFlow<Boolean> = _isSearching.asStateFlow()

    private val _isNowPlayingExpanded = MutableStateFlow(false)
    val isNowPlayingExpanded: StateFlow<Boolean> = _isNowPlayingExpanded.asStateFlow()

    private val _trackDetails = MutableStateFlow<Track?>(null)
    val trackDetails: StateFlow<Track?> = _trackDetails.asStateFlow()

    fun showTrackDetails(track: Track) {
        _trackDetails.value = track
    }

    fun dismissTrackDetails() {
        _trackDetails.value = null
    }

    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error.asStateFlow()

    private val _podcastQuery = MutableStateFlow("")
    val podcastQuery: StateFlow<String> = _podcastQuery.asStateFlow()

    private val _podcastShows = MutableStateFlow<List<PodcastShow>>(emptyList())
    val podcastShows: StateFlow<List<PodcastShow>> = _podcastShows.asStateFlow()

    private val _podcastLoading = MutableStateFlow(false)
    val podcastLoading: StateFlow<Boolean> = _podcastLoading.asStateFlow()

    private val _selectedPodcast = MutableStateFlow<PodcastShow?>(null)
    val selectedPodcast: StateFlow<PodcastShow?> = _selectedPodcast.asStateFlow()

    private val _podcastEpisodes = MutableStateFlow<List<PodcastEpisode>>(emptyList())
    val podcastEpisodes: StateFlow<List<PodcastEpisode>> = _podcastEpisodes.asStateFlow()

    private val _podcastEpisodesLoading = MutableStateFlow(false)
    val podcastEpisodesLoading: StateFlow<Boolean> = _podcastEpisodesLoading.asStateFlow()

    private val _storyQuery = MutableStateFlow("")
    val storyQuery: StateFlow<String> = _storyQuery.asStateFlow()

    private val _storyBooks = MutableStateFlow<List<StoryBook>>(emptyList())
    val storyBooks: StateFlow<List<StoryBook>> = _storyBooks.asStateFlow()

    private val _storyLoading = MutableStateFlow(false)
    val storyLoading: StateFlow<Boolean> = _storyLoading.asStateFlow()

    private val _selectedStory = MutableStateFlow<StoryBook?>(null)
    val selectedStory: StateFlow<StoryBook?> = _selectedStory.asStateFlow()

    private val _storyChapters = MutableStateFlow<List<StoryChapter>>(emptyList())
    val storyChapters: StateFlow<List<StoryChapter>> = _storyChapters.asStateFlow()

    private val _storyChaptersLoading = MutableStateFlow(false)
    val storyChaptersLoading: StateFlow<Boolean> = _storyChaptersLoading.asStateFlow()

    private val _spokenSpeed = MutableStateFlow(playbackSettings.spokenSpeed)
    val spokenSpeed: StateFlow<Float> = _spokenSpeed.asStateFlow()

    private val _skipSeconds = MutableStateFlow(playbackSettings.skipSeconds)
    val skipSeconds: StateFlow<Int> = _skipSeconds.asStateFlow()

    private val _resumeOnOpen = MutableStateFlow(playbackSettings.resumeOnOpen)
    val resumeOnOpen: StateFlow<Boolean> = _resumeOnOpen.asStateFlow()

    private val _sleepLabel = MutableStateFlow("Off")
    val sleepLabel: StateFlow<String> = _sleepLabel.asStateFlow()

    private val _sleepMode = MutableStateFlow("off")
    val sleepMode: StateFlow<String> = _sleepMode.asStateFlow()

    private var sleepJob: Job? = null
    private var sleepChoice = SleepChoice.OFF
    private var lastSessionSave = 0L

    private var searchDebounceJob: Job? = null

    val allTracks: StateFlow<List<Track>> = repository.allTracks
    val likedTracks: StateFlow<List<Track>> = repository.likedTracks
    val homeFeed: StateFlow<HomeFeed> = repository.homeFeed

    private val _selectedPlaylist = MutableStateFlow<AutoPlaylist?>(null)
    val selectedPlaylist: StateFlow<AutoPlaylist?> = _selectedPlaylist.asStateFlow()

    init {
        audioPlayer.onErrorCallback = { errorMessage ->
            val track = audioPlayer.playerState.value.currentTrack
            val msg = when {
                track?.isLocal == true && !checkAudioPermission() ->
                    "Storage permission not granted. Allow media access in Settings."
                track?.isLocal != true && !isOnline() ->
                    "No internet connection. Online songs unavailable."
                else -> errorMessage
            }
            _error.value = msg
            pushNotification("Playback issue", msg)
        }

        audioPlayer.onSleepFinished = {
            sleepChoice = SleepChoice.OFF
            _sleepMode.value = "off"
            _sleepLabel.value = "Off"
        }
        audioPlayer.preferredSpokenSpeed = playbackSettings.spokenSpeed

        audioPlayer.onListeningEvent = { track, action, positionMs ->
            if (action == "COMPLETE" && track.isSpokenWord) {
                podcastProgress.clear(track.id)
            }
            val listeningAction = when (action) {
                "COMPLETE" -> ListeningAction.COMPLETE
                "SKIP" -> ListeningAction.SKIP
                "QUEUE" -> ListeningAction.QUEUE
                else -> ListeningAction.PLAY
            }
            repository.recordListening(track, listeningAction, positionMs)
        }

        viewModelScope.launch {
            playerState.collect { state ->
                val track = state.currentTrack ?: return@collect
                val now = System.currentTimeMillis()
                if (state.queue.isNotEmpty() && now - lastSessionSave > 5_000) {
                    lastSessionSave = now
                    playbackSettings.saveSession(
                        state.queue,
                        state.currentQueueIndex.coerceAtLeast(0),
                        state.currentPositionMs
                    )
                }
                if (!track.isSpokenWord) return@collect
                if (state.durationMs > 0 && state.currentPositionMs >= state.durationMs - 3_000) {
                    podcastProgress.clear(track.id)
                    return@collect
                }
                if (state.currentPositionMs > 3_000 && now - lastProgressSave > 5_000) {
                    lastProgressSave = now
                    podcastProgress.save(track.id, state.currentPositionMs)
                }
            }
        }

        if (audioPlayer.playerState.value.currentTrack == null) {
            restoreLastSession()
        }
        refreshHome()
    }

    fun onPodcastQueryChange(value: String) {
        _podcastQuery.value = value
    }

    fun searchPodcasts(query: String) {
        val term = query.trim().ifBlank { "Hindi" }
        _podcastQuery.value = term
        podcastSearchJob?.cancel()
        podcastSearchJob = viewModelScope.launch {
            _podcastLoading.value = true
            try {
                val request = when {
                    term.contains("podcast", ignoreCase = true) -> term
                    else -> "$term podcast"
                }
                val shows = podcastEngine.searchShows(request)
                _podcastShows.value = shows
                if (shows.isEmpty()) _error.value = "No podcasts found"
            } catch (_: Exception) {
                _error.value = "Could not load podcasts"
            } finally {
                _podcastLoading.value = false
            }
        }
    }

    fun openPodcast(show: PodcastShow) {
        _selectedPodcast.value = show
        _podcastEpisodes.value = emptyList()
        podcastFeedJob?.cancel()
        podcastFeedJob = viewModelScope.launch {
            _podcastEpisodesLoading.value = true
            try {
                val episodes = podcastEngine.loadEpisodes(show)
                if (_selectedPodcast.value?.feedUrl == show.feedUrl) {
                    _podcastEpisodes.value = episodes
                    if (episodes.isEmpty()) _error.value = "No episodes in this show"
                }
            } catch (_: Exception) {
                _error.value = "Could not load episodes"
            } finally {
                _podcastEpisodesLoading.value = false
            }
        }
    }

    fun clearSelectedPodcast() {
        _selectedPodcast.value = null
        _podcastEpisodes.value = emptyList()
    }

    fun onStoryQueryChange(value: String) {
        _storyQuery.value = value
    }

    fun searchStories(query: String) {
        val term = query.trim().ifBlank { "Hindi" }
        _storyQuery.value = term
        storySearchJob?.cancel()
        storySearchJob = viewModelScope.launch {
            _storyLoading.value = true
            try {
                val books = storyEngine.loadShelf(term)
                _storyBooks.value = books
                if (books.isEmpty()) _error.value = "No stories found"
            } catch (_: Exception) {
                _error.value = "Could not load stories"
            } finally {
                _storyLoading.value = false
            }
        }
    }

    fun openStory(book: StoryBook) {
        _selectedStory.value = book
        _storyChapters.value = emptyList()
        storyChapterJob?.cancel()
        storyChapterJob = viewModelScope.launch {
            _storyChaptersLoading.value = true
            try {
                val chapters = storyEngine.loadChapters(book)
                if (_selectedStory.value?.id == book.id) {
                    _storyChapters.value = chapters
                    if (chapters.isEmpty()) _error.value = "No chapters in this story"
                }
            } catch (_: Exception) {
                _error.value = "Could not load chapters"
            } finally {
                _storyChaptersLoading.value = false
            }
        }
    }

    fun clearSelectedStory() {
        _selectedStory.value = null
        _storyChapters.value = emptyList()
    }

    fun playStoryChapter(index: Int) {
        val book = _selectedStory.value ?: return
        val chapters = _storyChapters.value
        if (index !in chapters.indices) return
        val tracks = chapters.map { it.toTrack(book) }
        val track = tracks[index]
        val resume = podcastProgress.get(track.id)
        startPlayback(tracks, index)
        val endGuard = if (track.durationMs > 20_000) track.durationMs - 10_000 else Long.MAX_VALUE
        if (resume in 8_000 until endGuard) {
            viewModelScope.launch {
                delay(800)
                if (playerState.value.currentTrack?.id == track.id) {
                    audioPlayer.seekToPosition(resume)
                }
            }
        }
    }

    fun playPodcastEpisode(index: Int) {
        val show = _selectedPodcast.value ?: return
        val episodes = _podcastEpisodes.value
        if (index !in episodes.indices) return
        val tracks = episodes.map { it.toTrack(show) }
        val track = tracks[index]
        val resume = podcastProgress.get(track.id)
        startPlayback(tracks, index)
        val endGuard = if (track.durationMs > 20_000) track.durationMs - 10_000 else Long.MAX_VALUE
        if (resume in 8_000 until endGuard) {
            viewModelScope.launch {
                delay(800)
                if (playerState.value.currentTrack?.id == track.id) {
                    audioPlayer.seekToPosition(resume)
                }
            }
        }
    }

    fun openPlaylist(playlist: AutoPlaylist) {
        _playlistVisibleCount.value = PAGE_SIZE
        _selectedPlaylist.value = playlist
    }

    fun openPlaylistById(id: String) {
        _playlistVisibleCount.value = PAGE_SIZE
        _selectedPlaylist.value = repository.getAutoPlaylist(id)
            ?: homeFeed.value.autoPlaylists.firstOrNull { it.id == id }
    }

    fun clearSelectedPlaylist() {
        _selectedPlaylist.value = null
        _playlistVisibleCount.value = PAGE_SIZE
    }

    fun playPlaylist(playlist: AutoPlaylist, startIndex: Int = 0) {
        if (playlist.tracks.isEmpty()) return
        startPlayback(playlist.tracks, startIndex)
        pushNotification(playlist.title, "Playing ${playlist.tracks.size} songs")
    }

    fun loadMorePlaylist() {
        val total = _selectedPlaylist.value?.tracks?.size ?: return
        val current = _playlistVisibleCount.value
        if (_isLoadingMorePlaylist.value || current >= total) return
        viewModelScope.launch {
            _isLoadingMorePlaylist.value = true
            delay(400)
            _playlistVisibleCount.value = (current + PAGE_SIZE).coerceAtMost(total)
            _isLoadingMorePlaylist.value = false
        }
    }

    fun refreshHome() {
        val hasContent = homeFeed.value.trending.isNotEmpty() ||
            homeFeed.value.quickAccess.isNotEmpty() ||
            homeFeed.value.localSongs.isNotEmpty()
        if (!hasContent) {
            _isHomeLoading.value = true
        }
        viewModelScope.launch {
            _isHomeLoading.value = true
            _trendingVisibleCount.value = PAGE_SIZE
            try {
                repository.loadHomeFeed()
                _localTracksFound.value = repository.homeFeed.value.localSongs.isNotEmpty()
                refreshSearchDiscovery()
                val trendingCount = repository.homeFeed.value.trending.size
                val localCount = repository.homeFeed.value.localSongs.size
                pushNotification(
                    "Home refreshed",
                    "Loaded $trendingCount trending + $localCount local songs"
                )
                if (trendingCount == 0 && !isOnline()) {
                    _error.value = "No internet. Showing local songs only."
                }
            } catch (e: Exception) {
                _error.value = "Could not load home feed: ${e.message}"
            } finally {
                _isHomeLoading.value = false
            }
        }
    }

    fun loadMoreTrending() {
        val total = homeFeed.value.trending.size
        val current = _trendingVisibleCount.value
        if (_isLoadingMoreTrending.value || current >= total) return

        viewModelScope.launch {
            _isLoadingMoreTrending.value = true
            delay(450)
            _trendingVisibleCount.value = (current + PAGE_SIZE).coerceAtMost(total)
            _isLoadingMoreTrending.value = false
        }
    }

    fun loadMoreSearch() {
        val total = _searchResults.value.size
        val current = _searchVisibleCount.value
        if (_isLoadingMoreSearch.value || current >= total) return

        viewModelScope.launch {
            _isLoadingMoreSearch.value = true
            delay(450)
            _searchVisibleCount.value = (current + PAGE_SIZE).coerceAtMost(total)
            _isLoadingMoreSearch.value = false
        }
    }

    fun loadMoreLibrary(totalSize: Int) {
        val current = _libraryVisibleCount.value
        if (_isLoadingMoreLibrary.value || current >= totalSize) return

        viewModelScope.launch {
            _isLoadingMoreLibrary.value = true
            delay(450)
            _libraryVisibleCount.value = (current + PAGE_SIZE).coerceAtMost(totalSize)
            _isLoadingMoreLibrary.value = false
        }
    }

    fun resetLibraryPagination() {
        _libraryVisibleCount.value = PAGE_SIZE
    }

    private fun isOnline(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            if (network == null) return false
            val caps = connectivityManager.getNetworkCapabilities(network)
            return caps != null && (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        }
        return connectivityManager.activeNetworkInfo?.isConnectedOrConnecting == true
    }

    fun scanLocalSongs() {
        checkAudioPermission()
        viewModelScope.launch {
            repository.refreshTracks()
            val localCount = repository.homeFeed.value.localSongs.size
            _localTracksFound.value = localCount > 0
            pushNotification("Library scanned", "$localCount songs found on this phone")
        }
    }

    fun refreshSearchDiscovery() {
        _browseCategories.value = repository.browseCategories()
        _popularSearches.value = repository.popularSearchTags()
    }

    fun onSearchQueryChange(query: String) {
        _error.value = null
        _searchQuery.value = query
        searchDebounceJob?.cancel()

        if (query.trim().isBlank()) {
            _searchSuggestions.value = emptyList()
            _searchResults.value = emptyList()
            _searchPlaylists.value = emptyList()
            repository.clearSearchPlaylists()
            _searchVisibleCount.value = PAGE_SIZE
            _isSearching.value = false
            refreshSearchDiscovery()
            return
        }

        searchDebounceJob = viewModelScope.launch {
            _isSearching.value = true

            if (!isOnline()) {
                _error.value = "No internet connection. Online search unavailable."
                _isSearching.value = false
                return@launch
            }

            launch {
                val suggestions = repository.getSearchSuggestions(query.trim())
                _searchSuggestions.value = suggestions
            }

            delay(400)

            val q = query.trim()
            val results = repository.searchSongs(q)
            _searchResults.value = results
            _searchPlaylists.value = repository.buildSearchPlaylists(q, results)
            _searchVisibleCount.value = PAGE_SIZE
            _isSearching.value = false
        }
    }

    fun submitSearch(query: String) {
        val q = query.trim()
        if (q.isBlank()) return
        _error.value = null
        _searchQuery.value = q
        searchDebounceJob?.cancel()
        repository.rememberSearchQuery(q)

        _searchResults.value = emptyList()
        _searchPlaylists.value = emptyList()
        _searchVisibleCount.value = PAGE_SIZE
        _isSearching.value = true

        if (!isOnline()) {
            _error.value = "No internet connection. Online search unavailable."
            _isSearching.value = false
            return
        }

        viewModelScope.launch {
            val results = repository.searchSongs(q)
            _searchResults.value = results
            _searchPlaylists.value = repository.buildSearchPlaylists(q, results)
            _searchVisibleCount.value = PAGE_SIZE
            _isSearching.value = false
            refreshSearchDiscovery()
        }
    }

    fun onSelectSuggestion(suggestion: String) {
        submitSearch(suggestion)
    }

    fun playTrack(track: Track) {
        val feed = homeFeed.value
        val candidateQueues = listOf(
            _searchResults.value,
            likedTracks.value,
            feed.trending,
            feed.madeForYou,
            feed.localSongs,
            allTracks.value,
            playerState.value.queue
        )

        val matchingQueue = candidateQueues.firstOrNull { queue ->
            queue.any { it.id == track.id }
        }

        if (matchingQueue != null) {
            val index = matchingQueue.indexOfFirst { it.id == track.id }
            startPlayback(matchingQueue, index)
        } else {
            startPlayback(listOf(track), 0)
        }
    }

    fun playTrackList(trackList: List<Track>, startIndex: Int) {
        startPlayback(trackList, startIndex)
    }

    private fun startPlayback(tracks: List<Track>, startIndex: Int) {
        if (tracks.isEmpty()) return
        audioPlayer.playTrackList(tracks, startIndex.coerceIn(0, tracks.lastIndex))
        setNowPlayingExpanded(true)
    }

    fun removeFromQueue(index: Int) {
        audioPlayer.removeFromQueue(index)
    }

    fun clearQueue() {
        val size = playerState.value.queue.size
        if (size == 0) return
        audioPlayer.clearQueue()
        pushNotification("Queue cleared", "Removed $size songs from queue")
    }

    fun togglePlayPause() {
        audioPlayer.togglePlayPause()
    }

    fun playNext() {
        audioPlayer.playNext()
    }

    fun playPrevious() {
        audioPlayer.playPrevious()
    }

    fun seekTo(positionMs: Long) {
        audioPlayer.seekToPosition(positionMs)
    }

    fun cyclePlaybackSpeed() {
        val next = nextSpokenSpeed(playerState.value.playbackSpeed)
        audioPlayer.setPlaybackSpeed(next)
        if (playerState.value.currentTrack?.isSpokenWord == true) {
            rememberSpokenSpeed(next)
        }
    }

    fun cycleSpokenSpeed() {
        val next = nextSpokenSpeed(_spokenSpeed.value)
        rememberSpokenSpeed(next)
        if (playerState.value.currentTrack?.isSpokenWord == true) {
            audioPlayer.setPlaybackSpeed(next)
        }
    }

    fun selectSpokenSpeed(speed: Float) {
        if (speed !in PlaybackSettingsStore.SPEEDS) return
        rememberSpokenSpeed(speed)
        if (playerState.value.currentTrack?.isSpokenWord == true) {
            audioPlayer.setPlaybackSpeed(speed)
        }
    }

    fun selectSkipSeconds(seconds: Int) {
        if (seconds !in PlaybackSettingsStore.SKIPS) return
        _skipSeconds.value = seconds
        playbackSettings.skipSeconds = seconds
    }

    fun selectSleep(mode: String) {
        val choice = when (mode) {
            "15" -> SleepChoice.MIN_15
            "30" -> SleepChoice.MIN_30
            "45" -> SleepChoice.MIN_45
            "end" -> SleepChoice.END
            else -> SleepChoice.OFF
        }
        applySleep(choice)
    }

    fun skipForward() {
        audioPlayer.seekBy(_skipSeconds.value * 1_000L)
    }

    fun skipBackward() {
        audioPlayer.seekBy(-_skipSeconds.value * 1_000L)
    }

    fun setResumeOnOpen(enabled: Boolean) {
        _resumeOnOpen.value = enabled
        playbackSettings.resumeOnOpen = enabled
    }

    fun cycleSleepTimer() {
        val next = when (sleepChoice) {
            SleepChoice.OFF -> SleepChoice.MIN_15
            SleepChoice.MIN_15 -> SleepChoice.MIN_30
            SleepChoice.MIN_30 -> SleepChoice.MIN_45
            SleepChoice.MIN_45 -> SleepChoice.END
            SleepChoice.END -> SleepChoice.OFF
        }
        applySleep(next)
    }

    private fun rememberSpokenSpeed(speed: Float) {
        _spokenSpeed.value = speed
        playbackSettings.spokenSpeed = speed
        audioPlayer.preferredSpokenSpeed = speed
    }

    private fun nextSpokenSpeed(current: Float): Float {
        val speeds = PlaybackSettingsStore.SPEEDS
        val index = speeds.indexOfFirst { kotlin.math.abs(it - current) < 0.01f }
        return speeds[(index + 1).mod(speeds.size)]
    }

    private fun applySleep(choice: SleepChoice) {
        sleepJob?.cancel()
        sleepChoice = choice
        _sleepMode.value = when (choice) {
            SleepChoice.OFF -> "off"
            SleepChoice.MIN_15 -> "15"
            SleepChoice.MIN_30 -> "30"
            SleepChoice.MIN_45 -> "45"
            SleepChoice.END -> "end"
        }
        audioPlayer.stopAfterCurrent = choice == SleepChoice.END
        when (choice) {
            SleepChoice.OFF -> _sleepLabel.value = "Off"
            SleepChoice.END -> _sleepLabel.value = "Stops at the end of this track"
            SleepChoice.MIN_15, SleepChoice.MIN_30, SleepChoice.MIN_45 -> {
                val minutes = when (choice) {
                    SleepChoice.MIN_15 -> 15
                    SleepChoice.MIN_30 -> 30
                    else -> 45
                }
                val endsAt = System.currentTimeMillis() + minutes * 60_000L
                sleepJob = viewModelScope.launch {
                    while (true) {
                        val left = endsAt - System.currentTimeMillis()
                        if (left <= 0L) {
                            audioPlayer.pausePlayback()
                            applySleep(SleepChoice.OFF)
                            pushNotification("Sleep timer", "Playback paused")
                            return@launch
                        }
                        val mins = ((left + 59_999L) / 60_000L).toInt()
                        _sleepLabel.value = "$mins min left"
                        delay(1_000)
                    }
                }
            }
        }
    }

    private fun restoreLastSession() {
        if (!playbackSettings.resumeOnOpen) return
        val session = playbackSettings.loadSession() ?: return
        val track = session.tracks[session.index]
        val endGuard = if (track.durationMs > 20_000) track.durationMs - 10_000 else Long.MAX_VALUE
        val position = if (session.positionMs in 8_000 until endGuard) session.positionMs else 0L
        audioPlayer.playTrackList(session.tracks, session.index, position)
    }

    fun seekBy(deltaMs: Long) {
        audioPlayer.seekBy(deltaMs)
    }

    fun toggleShuffle() {
        audioPlayer.toggleShuffle()
    }

    fun toggleRepeat() {
        audioPlayer.toggleRepeat()
    }

    fun toggleLike(track: Track) {
        val wasLiked = likedTrackIds.value.contains(track.id)
        repository.toggleLike(track)
        if (!wasLiked) {
            pushNotification("Liked", "${track.title} added to Liked Songs")
        }
    }

    fun toggleLike(trackId: String) {
        repository.toggleLike(trackId)
    }

    fun clearLikedSongs() {
        val count = likedTracks.value.size
        repository.clearLikedTracks()
        pushNotification("Liked songs cleared", "Removed $count songs from likes")
    }

    fun setNowPlayingExpanded(expanded: Boolean) {
        _isNowPlayingExpanded.value = expanded
    }

    fun clearError() {
        _error.value = null
    }

    fun clearNotifications() {
        _notifications.value = emptyList()
    }

    private fun pushNotification(title: String, message: String) {
        _notifications.update { current ->
            (listOf(AppNotification(title = title, message = message)) + current).take(30)
        }
    }
}

private enum class SleepChoice {
    OFF, MIN_15, MIN_30, MIN_45, END
}
