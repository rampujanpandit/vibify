package com.vibify.music.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import com.vibify.music.data.model.Track
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "VibifyAudioPlayer"

typealias OnError = (String) -> Unit
typealias OnListeningEvent = (track: Track, action: String, positionMs: Long) -> Unit

class VibifyAudioPlayer private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var instance: VibifyAudioPlayer? = null

        fun getInstance(context: Context): VibifyAudioPlayer {
            return instance ?: synchronized(this) {
                instance ?: VibifyAudioPlayer(context.applicationContext).also { instance = it }
            }
        }
    }

    var onErrorCallback: OnError? = null
    var onListeningEvent: OnListeningEvent? = null

    private var lastEmittedPlayTrackId: String? = null
    private var errorRetryMediaId: String? = null

    val exoPlayerInternal: ExoPlayer
        get() = exoPlayer

    private val playerScope = CoroutineScope(Dispatchers.Main)
    private var progressJob: Job? = null

    private val audioAttributes = AudioAttributes.Builder()
        .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
        .setUsage(C.USAGE_MEDIA)
        .build()

    // Custom HTTP factory so CDN streams (Saavn etc.) accept the request and don't time out quickly
    private val httpDataSourceFactory = DefaultHttpDataSource.Factory()
        .setUserAgent("Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Vibify/1.0")
        .setConnectTimeoutMs(15_000)
        .setReadTimeoutMs(45_000)
        .setAllowCrossProtocolRedirects(true)

    private val dataSourceFactory = DefaultDataSource.Factory(context, httpDataSourceFactory)

    private val loadControl = DefaultLoadControl.Builder()
        .setBufferDurationsMs(20_000, 90_000, 1_500, 4_000)
        .build()

    private val exoPlayer: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(audioAttributes, /* handleAudioFocus= */ true)
        .setHandleAudioBecomingNoisy(true)
        .setLoadControl(loadControl)
        .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
        .build()

    private val _playerState = MutableStateFlow(PlayerState())
    val playerState: StateFlow<PlayerState> = _playerState.asStateFlow()

    init {
        exoPlayer.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Log.d(TAG, "isPlayingChanged: $isPlaying playWhenReady=${exoPlayer.playWhenReady}")
                _playerState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        // Only show spinner while waiting to start, not during normal prefetch
                        isLoading = !isPlaying && exoPlayer.playWhenReady &&
                            exoPlayer.playbackState == Player.STATE_BUFFERING
                    )
                }
                if (isPlaying || exoPlayer.playWhenReady) {
                    startProgressTracker()
                } else {
                    stopProgressTracker()
                }
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                Log.d(TAG, "playbackStateChanged: $playbackState")
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else _playerState.value.durationMs
                val waitingToPlay = exoPlayer.playWhenReady &&
                    !exoPlayer.isPlaying &&
                    playbackState == Player.STATE_BUFFERING

                _playerState.update {
                    it.copy(
                        isLoading = waitingToPlay,
                        durationMs = duration,
                        isPlaying = exoPlayer.isPlaying
                    )
                }

                if (playbackState == Player.STATE_READY && exoPlayer.playWhenReady) {
                    startProgressTracker()
                }

                if (playbackState == Player.STATE_ENDED) {
                    _playerState.value.currentTrack?.let { track ->
                        onListeningEvent?.invoke(track, "COMPLETE", exoPlayer.currentPosition)
                    }
                    when (_playerState.value.repeatMode) {
                        RepeatMode.ONE -> {
                            exoPlayer.seekTo(0L)
                            exoPlayer.play()
                        }
                        RepeatMode.ALL -> {
                            playNext(fromUser = false)
                        }
                        RepeatMode.OFF -> {
                            val state = _playerState.value
                            if (state.currentQueueIndex + 1 < state.queue.size) {
                                playNext(fromUser = false)
                            } else {
                                exoPlayer.pause()
                            }
                        }
                    }
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                Log.d(TAG, "mediaItemTransition: title=${mediaItem?.mediaMetadata?.title}")
                val currentIndex = exoPlayer.currentMediaItemIndex
                val queue = _playerState.value.queue
                if (currentIndex in queue.indices) {
                    val track = queue[currentIndex]
                    val duration = if (exoPlayer.duration > 0) exoPlayer.duration else track.durationMs
                    _playerState.update {
                        it.copy(
                            currentQueueIndex = currentIndex,
                            currentTrack = track,
                            currentPositionMs = 0L,
                            durationMs = duration
                        )
                    }
                    if (lastEmittedPlayTrackId != track.id) {
                        lastEmittedPlayTrackId = track.id
                        onListeningEvent?.invoke(track, "PLAY", 0L)
                    }
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                Log.e(TAG, "Player error: ${error.errorCodeName} / ${error.message}", error)
                val mediaId = exoPlayer.currentMediaItem?.mediaId
                val resumeAt = exoPlayer.currentPosition.coerceAtLeast(0L)
                if (mediaId != null && errorRetryMediaId != mediaId) {
                    errorRetryMediaId = mediaId
                    Log.w(TAG, "Retrying stream once at ${resumeAt}ms")
                    playerScope.launch {
                        exoPlayer.prepare()
                        if (resumeAt > 1_000L) exoPlayer.seekTo(resumeAt)
                        exoPlayer.play()
                    }
                    return
                }
                errorRetryMediaId = null
                _playerState.update {
                    it.copy(
                        isLoading = false,
                        isPlaying = false
                    )
                }
                onErrorCallback?.invoke("Playback failed: ${error.message ?: error.errorCodeName}")
            }

            override fun onIsLoadingChanged(isLoading: Boolean) {
                // Ignore prefetch loading — MiniPlayer was stuck on spinner while audio played
                Log.d(TAG, "isLoadingChanged(prefetch): $isLoading isPlaying=${exoPlayer.isPlaying}")
            }
        })

        Log.d(TAG, "VibifyAudioPlayer initialized")
    }

    private fun trackToMediaItem(track: Track): MediaItem {
        val metadata = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
            .setArtworkUri(track.artworkUri?.let { Uri.parse(it) })
            .build()

        return MediaItem.Builder()
            .setUri(stablePlaybackUri(track.mediaUri))
            .setMediaId(track.id)
            .setMediaMetadata(metadata)
            .build()
    }

    /** Leave signed and preview links unchanged. Only repair old 160kbps links that die mid-song. */
    private fun stablePlaybackUri(uri: Uri): Uri {
        val raw = uri.toString()
        if ('?' in raw || raw.contains("preview.saavncdn.com")) return uri
        val fixed = raw
            .replace("_160.mp4", "_96.mp4")
            .replace("_160.m4a", "_96.m4a")
        return if (fixed == raw) uri else Uri.parse(fixed)
    }

    fun playTrackList(tracks: List<Track>, startIndex: Int = 0) {
        if (tracks.isEmpty()) {
            Log.e(TAG, "playTrackList: empty tracks")
            onErrorCallback?.invoke("No tracks to play")
            return
        }
        if (startIndex !in tracks.indices) {
            Log.e(TAG, "playTrackList: startIndex $startIndex out of bounds (size=${tracks.size})")
            return
        }

        val startTrack = tracks[startIndex]
        val speed = if (startTrack.isSpokenWord) {
            _playerState.value.playbackSpeed.takeIf { it > 0f } ?: 1f
        } else {
            1f
        }
        exoPlayer.setPlaybackSpeed(speed)
        Log.d(TAG, "playTrackList: track=${startTrack.title}, uri=${startTrack.mediaUri}")

        val mediaItems = tracks.map { track ->
            Log.d(TAG, "  mediaItem: ${track.title} -> ${track.mediaUri}")
            trackToMediaItem(track)
        }

        _playerState.update {
            it.copy(
                queue = tracks,
                currentQueueIndex = startIndex,
                currentTrack = startTrack,
                isPlaying = true,
                isLoading = true,
                currentPositionMs = 0L,
                durationMs = startTrack.durationMs,
                playbackSpeed = speed
            )
        }

        exoPlayer.playWhenReady = true
        exoPlayer.setMediaItems(mediaItems, startIndex, 0L)
        exoPlayer.prepare()
        exoPlayer.play()
        errorRetryMediaId = null
        ensurePlaybackService()
        Log.d(TAG, "exoPlayer.play() called")
    }

    fun showLoading(track: Track) {
        _playerState.update {
            it.copy(
                currentTrack = track,
                isLoading = true,
                isPlaying = false,
                currentPositionMs = 0L
            )
        }
    }

    /** Swap in full-length links for songs that are still on a 30-second preview. */
    fun refreshQueueUris(tracks: List<Track>) {
        val state = _playerState.value
        if (tracks.isEmpty() || state.queue.size != tracks.size) return
        if (state.queue.map { it.id } != tracks.map { it.id }) return

        tracks.forEachIndexed { index, track ->
            if (state.queue[index].mediaUri == track.mediaUri) return@forEachIndexed
            if (index >= exoPlayer.mediaItemCount) return@forEachIndexed
            val item = trackToMediaItem(track)
            if (index == exoPlayer.currentMediaItemIndex) {
                val playing = exoPlayer.currentMediaItem?.localConfiguration?.uri?.toString().orEmpty()
                if (!isShortPreview(playing)) return@forEachIndexed
                val position = exoPlayer.currentPosition.coerceAtLeast(0L)
                exoPlayer.replaceMediaItem(index, item)
                if (position > 0L) exoPlayer.seekTo(position)
                exoPlayer.play()
            } else {
                exoPlayer.replaceMediaItem(index, item)
            }
        }
        val playingIndex = exoPlayer.currentMediaItemIndex.coerceIn(0, tracks.lastIndex)
        _playerState.update {
            it.copy(
                queue = tracks,
                currentQueueIndex = playingIndex,
                currentTrack = tracks[playingIndex]
            )
        }
    }

    private fun isShortPreview(uri: String): Boolean {
        return uri.contains("preview.saavncdn.com") ||
            uri.contains("_96_p.") ||
            uri.contains("itunes.apple.com") ||
            uri.contains("audio-ssl.itunes")
    }

    private fun ensurePlaybackService() {
        val intent = Intent(context, PlaybackService::class.java)
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Could not start playback notification service", e)
        }
    }

    fun playTrack(track: Track) {
        val currentQueue = _playerState.value.queue
        val existingIndex = currentQueue.indexOfFirst { it.id == track.id }

        if (existingIndex != -1) {
            playTrackList(currentQueue, existingIndex)
        } else {
            val newQueue = listOf(track) + currentQueue
            playTrackList(newQueue, 0)
        }
    }

    fun togglePlayPause() {
        Log.d(TAG, "togglePlayPause: isPlaying=${exoPlayer.isPlaying}, playWhenReady=${exoPlayer.playWhenReady}, state=${exoPlayer.playbackState}")
        if (exoPlayer.isPlaying) {
            exoPlayer.pause()
        } else {
            if (exoPlayer.mediaItemCount == 0) {
                val track = _playerState.value.currentTrack
                val queue = _playerState.value.queue
                if (queue.isNotEmpty()) {
                    val index = _playerState.value.currentQueueIndex.coerceIn(0, queue.lastIndex)
                    playTrackList(queue, index)
                } else if (track != null) {
                    playTrack(track)
                }
            } else {
                if (exoPlayer.playbackState == Player.STATE_IDLE) {
                    exoPlayer.prepare()
                }
                exoPlayer.play()
                ensurePlaybackService()
            }
        }
    }

    fun playNext(fromUser: Boolean = true) {
        Log.d(TAG, "playNext fromUser=$fromUser")
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        if (fromUser) {
            maybeEmitSkip(state.currentTrack)
        }

        if (exoPlayer.hasNextMediaItem()) {
            exoPlayer.seekToNextMediaItem()
            exoPlayer.play()
        } else {
            if (state.repeatMode == RepeatMode.ALL) {
                exoPlayer.seekTo(0, 0L)
                exoPlayer.play()
            }
        }
    }

    fun playPrevious() {
        Log.d(TAG, "playPrevious")
        val state = _playerState.value
        if (state.queue.isEmpty()) return

        if (state.repeatMode == RepeatMode.ONE) {
            exoPlayer.seekTo(0L)
            exoPlayer.play()
            return
        }

        if (exoPlayer.currentPosition > 3000) {
            exoPlayer.seekTo(0L)
            return
        }

        maybeEmitSkip(state.currentTrack)

        if (exoPlayer.hasPreviousMediaItem()) {
            exoPlayer.seekToPreviousMediaItem()
            exoPlayer.play()
        } else {
            val lastIndex = state.queue.size - 1
            if (lastIndex >= 0) {
                exoPlayer.seekTo(lastIndex, 0L)
                exoPlayer.play()
            }
        }
    }

    private fun maybeEmitSkip(track: Track?) {
        if (track == null) return
        val pos = exoPlayer.currentPosition
        if (pos in 1 until 30_000) {
            onListeningEvent?.invoke(track, "SKIP", pos)
        }
    }

    fun seekToPosition(positionMs: Long) {
        exoPlayer.seekTo(positionMs.coerceAtLeast(0L))
        _playerState.update { it.copy(currentPositionMs = positionMs.coerceAtLeast(0L)) }
    }

    fun seekBy(deltaMs: Long) {
        val duration = exoPlayer.duration.takeIf { it > 0 } ?: Long.MAX_VALUE
        val next = (exoPlayer.currentPosition + deltaMs).coerceIn(0L, duration)
        seekToPosition(next)
    }

    fun setPlaybackSpeed(speed: Float) {
        exoPlayer.setPlaybackSpeed(speed)
        _playerState.update { it.copy(playbackSpeed = speed) }
    }

    fun toggleShuffle() {
        val state = _playerState.value
        val newShuffle = !state.isShuffle
        exoPlayer.shuffleModeEnabled = newShuffle

        if (newShuffle) {
            val currentTrack = state.currentTrack
            val currentPos = exoPlayer.currentPosition.coerceAtLeast(0L)
            val shuffledQueue = state.queue.shuffled()
            val currentIndex = shuffledQueue.indexOfFirst { it.id == (currentTrack?.id ?: "") }
                .let { if (it != -1) it else 0 }

            _playerState.update {
                it.copy(
                    queue = shuffledQueue,
                    currentQueueIndex = currentIndex,
                    isShuffle = true
                )
            }

            exoPlayer.setMediaItems(
                shuffledQueue.map { trackToMediaItem(it) },
                currentIndex,
                currentPos
            )
            exoPlayer.prepare()
            if (state.isPlaying || exoPlayer.playWhenReady) {
                exoPlayer.play()
            }
        } else {
            _playerState.update { it.copy(isShuffle = false) }
        }
    }

    fun cycleRepeatMode(): RepeatMode {
        val nextMode = when (_playerState.value.repeatMode) {
            RepeatMode.OFF -> RepeatMode.ALL
            RepeatMode.ALL -> RepeatMode.ONE
            RepeatMode.ONE -> RepeatMode.OFF
        }

        exoPlayer.repeatMode = when (nextMode) {
            RepeatMode.OFF -> Player.REPEAT_MODE_OFF
            RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            RepeatMode.ONE -> Player.REPEAT_MODE_ONE
        }

        _playerState.update { it.copy(repeatMode = nextMode) }
        return nextMode
    }

    fun toggleRepeat() {
        cycleRepeatMode()
    }

    fun clearQueue() {
        exoPlayer.stop()
        exoPlayer.clearMediaItems()
        _playerState.value = PlayerState()
    }

    fun removeFromQueue(index: Int) {
        val state = _playerState.value
        if (index !in state.queue.indices) return

        val newQueue = state.queue.toMutableList().also { it.removeAt(index) }
        if (newQueue.isEmpty()) {
            clearQueue()
            return
        }

        val newIndex = when {
            index < state.currentQueueIndex -> state.currentQueueIndex - 1
            index == state.currentQueueIndex -> index.coerceAtMost(newQueue.lastIndex)
            else -> state.currentQueueIndex
        }.coerceIn(0, newQueue.lastIndex)

        val keepPlaying = exoPlayer.isPlaying || exoPlayer.playWhenReady
        val position = if (index == state.currentQueueIndex) 0L else exoPlayer.currentPosition.coerceAtLeast(0L)

        _playerState.update {
            it.copy(
                queue = newQueue,
                currentQueueIndex = newIndex,
                currentTrack = newQueue[newIndex]
            )
        }

        exoPlayer.setMediaItems(newQueue.map { trackToMediaItem(it) }, newIndex, position)
        exoPlayer.prepare()
        if (keepPlaying) exoPlayer.play()
    }

    private fun startProgressTracker() {
        stopProgressTracker()
        progressJob = playerScope.launch {
            while (isActive) {
                val currentPos = exoPlayer.currentPosition
                val duration = if (exoPlayer.duration > 0) exoPlayer.duration else 0L

                _playerState.update {
                    it.copy(
                        currentPositionMs = currentPos,
                        durationMs = duration
                    )
                }
                delay(500)
            }
        }
    }

    private fun stopProgressTracker() {
        progressJob?.cancel()
        progressJob = null
    }

    fun release() {
        stopProgressTracker()
        exoPlayer.release()
    }
}
