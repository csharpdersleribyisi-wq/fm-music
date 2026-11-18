package com.example.player

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.example.data.catalog.MusicCatalog
import com.example.data.model.Track
import com.example.data.offline.OfflineDownloadManager
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class PlaybackState(
    val currentTrack: Track? = null,
    val queue: List<Track> = emptyList(),
    val currentIndex: Int = -1,
    val isPlaying: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val shuffleEnabled: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_ALL,
    val playingFromOfflineVault: Boolean = false,
    val statusNotice: String? = null
)

class SonoraPlayerController(
    context: Context,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val onTrackStarted: (Track) -> Unit = {}
) {
    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null
    private var positionTickerJob: Job? = null

    private val _state = MutableStateFlow(PlaybackState())
    val state: StateFlow<PlaybackState> = _state.asStateFlow()

    private val _equalizerState = MutableStateFlow(EqualizerStore.load(appContext))
    val equalizerState: StateFlow<EqualizerSettings> = _equalizerState.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _state.update { it.copy(isPlaying = isPlaying) }
            if (isPlaying) startPositionTicker() else stopPositionTicker()
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val trackId = mediaItem?.mediaId ?: return
            val track = MusicCatalog.getTrack(trackId) ?: return
            val idx = _state.value.queue.indexOfFirst { it.id == trackId }
            val uri = mediaItem.localConfiguration?.uri
            _state.update {
                it.copy(
                    currentTrack = track,
                    currentIndex = idx,
                    durationMs = track.durationMs,
                    positionMs = 0L,
                    playingFromOfflineVault = uri?.scheme == "file"
                )
            }
            onTrackStarted(track)
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val ctrl = mediaController ?: return
            if (playbackState == Player.STATE_READY) {
                val dur = ctrl.duration.takeIf { it > 0 } ?: _state.value.currentTrack?.durationMs ?: 0L
                _state.update { it.copy(durationMs = dur) }
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _state.update { it.copy(shuffleEnabled = shuffleModeEnabled) }
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _state.update { it.copy(repeatMode = repeatMode) }
        }
    }

    init {
        connect()
    }

    private fun connect() {
        val token = SessionToken(appContext, ComponentName(appContext, SonoraPlaybackService::class.java))
        val future = MediaController.Builder(appContext, token).buildAsync()
        controllerFuture = future
        future.addListener({
            try {
                val controller = future.get()
                mediaController = controller
                controller.addListener(playerListener)
                controller.repeatMode = _state.value.repeatMode
                controller.shuffleModeEnabled = _state.value.shuffleEnabled
                pushEqualizerToService(controller, _equalizerState.value)
            } catch (_: Exception) {
                // Service may not be ready in test environments
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun playQueue(
        tracks: List<Track>,
        startTrack: Track,
        offlineModeEnabled: Boolean
    ) {
        scope.launch {
            val resolvedPairs = mutableListOf<Pair<Track, Uri>>()
            for (t in tracks) {
                val uri = offlineDownloadManager.resolvePlaybackUri(t, offlineModeEnabled)
                if (uri != null) {
                    resolvedPairs.add(t to uri)
                }
            }

            if (resolvedPairs.isEmpty()) {
                _state.update {
                    it.copy(
                        statusNotice = if (offlineModeEnabled) {
                            "Offline Mode is active. Download tracks first to listen offline."
                        } else {
                            "No playable tracks available."
                        }
                    )
                }
                return@launch
            }

            val startIndex = resolvedPairs.indexOfFirst { it.first.id == startTrack.id }
                .let { idx -> if (idx >= 0) idx else 0 }

            val targetTrack = resolvedPairs[startIndex].first
            if (targetTrack.id != startTrack.id && offlineModeEnabled) {
                _state.update {
                    it.copy(statusNotice = "${startTrack.title} isn't downloaded. Playing next offline track.")
                }
            }

            val mediaItems = resolvedPairs.map { (t, uri) ->
                MediaItem.Builder()
                    .setMediaId(t.id)
                    .setUri(uri)
                    .setMediaMetadata(
                        MediaMetadata.Builder()
                            .setTitle(t.title)
                            .setArtist(t.artist)
                            .setAlbumTitle(t.album)
                            .build()
                    )
                    .build()
            }

            val playableQueue = resolvedPairs.map { it.first }
            val isOfflineFile = resolvedPairs[startIndex].second.scheme == "file"

            _state.update {
                it.copy(
                    currentTrack = targetTrack,
                    queue = playableQueue,
                    currentIndex = startIndex,
                    durationMs = targetTrack.durationMs,
                    positionMs = 0L,
                    playingFromOfflineVault = isOfflineFile
                )
            }

            val ctrl = mediaController
            if (ctrl != null) {
                ctrl.setMediaItems(mediaItems, startIndex, 0L)
                ctrl.prepare()
                ctrl.play()
            }
            onTrackStarted(targetTrack)
        }
    }

    fun togglePlayPause() {
        val ctrl = mediaController ?: return
        if (ctrl.isPlaying) ctrl.pause() else ctrl.play()
    }

    fun seekTo(positionMs: Long) {
        val ctrl = mediaController ?: return
        val clamped = positionMs.coerceIn(0L, _state.value.durationMs.coerceAtLeast(1L))
        ctrl.seekTo(clamped)
        _state.update { it.copy(positionMs = clamped) }
    }

    fun skipNext() {
        val ctrl = mediaController ?: return
        if (ctrl.hasNextMediaItem()) {
            ctrl.seekToNextMediaItem()
        } else if (ctrl.mediaItemCount > 0) {
            ctrl.seekTo(0, 0L)
        }
    }

    fun skipPrevious() {
        val ctrl = mediaController ?: return
        if (ctrl.currentPosition > 3000L) {
            ctrl.seekTo(0L)
        } else if (ctrl.hasPreviousMediaItem()) {
            ctrl.seekToPreviousMediaItem()
        } else if (ctrl.mediaItemCount > 0) {
            ctrl.seekTo(ctrl.mediaItemCount - 1, 0L)
        }
    }

    fun toggleShuffle() {
        val next = !_state.value.shuffleEnabled
        mediaController?.shuffleModeEnabled = next
        _state.update { it.copy(shuffleEnabled = next) }
    }

    fun cycleRepeatMode() {
        val next = when (_state.value.repeatMode) {
            Player.REPEAT_MODE_ALL -> Player.REPEAT_MODE_ONE
            Player.REPEAT_MODE_ONE -> Player.REPEAT_MODE_OFF
            else -> Player.REPEAT_MODE_ALL
        }
        mediaController?.repeatMode = next
        _state.update { it.copy(repeatMode = next) }
    }

    fun clearNotice() {
        _state.update { it.copy(statusNotice = null) }
    }

    fun selectEqualizerPreset(preset: EqualizerPreset) {
        if (preset == EqualizerPreset.CUSTOM) return
        applyEqualizerUpdate(
            _equalizerState.value.copy(
                enabled = true,
                preset = preset,
                bandGainsDb = preset.bandGainsDb,
                preampDb = preset.preampDb
            )
        )
    }

    fun updateEqualizerBand(bandIndex: Int, gainDb: Float) {
        val current = _equalizerState.value
        val updatedGains = current.normalizedGains().toMutableList()
        if (bandIndex !in updatedGains.indices) return
        updatedGains[bandIndex] = gainDb.coerceIn(
            EqualizerSettings.MIN_GAIN_DB,
            EqualizerSettings.MAX_GAIN_DB
        )
        applyEqualizerUpdate(
            current.copy(
                enabled = true,
                preset = EqualizerPreset.CUSTOM,
                bandGainsDb = updatedGains
            )
        )
    }

    fun setEqualizerEnabled(enabled: Boolean) {
        applyEqualizerUpdate(_equalizerState.value.copy(enabled = enabled))
    }

    fun setEqualizerPreamp(preampDb: Float) {
        applyEqualizerUpdate(
            _equalizerState.value.copy(
                preampDb = preampDb.coerceIn(
                    EqualizerSettings.PREAMP_MIN_DB,
                    EqualizerSettings.PREAMP_MAX_DB
                )
            )
        )
    }

    fun resetEqualizer() {
        applyEqualizerUpdate(
            EqualizerSettings(
                enabled = true,
                preset = EqualizerPreset.FLAT,
                bandGainsDb = EqualizerPreset.FLAT.bandGainsDb,
                preampDb = 0f
            )
        )
        _state.update { it.copy(statusNotice = "Equalizer reset to Flat") }
    }

    private fun applyEqualizerUpdate(next: EqualizerSettings) {
        _equalizerState.value = next
        EqualizerStore.save(appContext, next)
        mediaController?.let { ctrl ->
            pushEqualizerToService(ctrl, next)
        }
    }

    private fun pushEqualizerToService(
        controller: MediaController,
        settings: EqualizerSettings
    ) {
        runCatching {
            controller.sendCustomCommand(
                SessionCommand(EqualizerProtocol.COMMAND_APPLY_EQUALIZER, Bundle.EMPTY),
                settings.toBundle()
            )
        }
    }

    private fun startPositionTicker() {
        if (positionTickerJob?.isActive == true) return
        positionTickerJob = scope.launch {
            while (isActive) {
                val ctrl = mediaController
                if (ctrl != null) {
                    _state.update {
                        it.copy(
                            positionMs = ctrl.currentPosition.coerceAtLeast(0L),
                            durationMs = ctrl.duration.takeIf { d -> d > 0 } ?: it.durationMs
                        )
                    }
                }
                delay(250)
            }
        }
    }

    private fun stopPositionTicker() {
        positionTickerJob?.cancel()
        positionTickerJob = null
    }

    fun release() {
        stopPositionTicker()
        mediaController?.removeListener(playerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }
        mediaController = null
    }
}
