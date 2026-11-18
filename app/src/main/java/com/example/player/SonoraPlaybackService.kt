package com.example.player

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SonoraPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null
    private lateinit var equalizerProcessor: EqualizerAudioProcessor
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var sleepTimerJob: Job? = null

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val initialEq = EqualizerStore.load(this)
        equalizerProcessor = EqualizerAudioProcessor(initialEq)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val renderersFactory = object : DefaultRenderersFactory(this) {
            override fun buildAudioSink(
                context: Context,
                enableFloatOutput: Boolean,
                enableAudioOutputPlaybackParams: Boolean
            ): AudioSink {
                return DefaultAudioSink.Builder(context)
                    .setEnableFloatOutput(enableFloatOutput)
                    .setEnableAudioTrackPlaybackParams(enableAudioOutputPlaybackParams)
                    .setAudioProcessors(arrayOf(equalizerProcessor))
                    .build()
            }
        }

        val player = ExoPlayer.Builder(this, renderersFactory)
            .setAudioAttributes(audioAttributes, true)
            .setHandleAudioBecomingNoisy(true)
            .build()

        val callback = object : MediaSession.Callback {
            override fun onConnect(
                session: MediaSession,
                controller: MediaSession.ControllerInfo
            ): MediaSession.ConnectionResult {
                val base = super.onConnect(session, controller)
                val updatedCommands = base.availableSessionCommands
                    .buildUpon()
                    .add(SessionCommand(EqualizerProtocol.COMMAND_APPLY_EQUALIZER, Bundle.EMPTY))
                    .add(SessionCommand(SleepTimerProtocol.COMMAND_SET_SLEEP_TIMER, Bundle.EMPTY))
                    .add(SessionCommand(SleepTimerProtocol.COMMAND_CANCEL_SLEEP_TIMER, Bundle.EMPTY))
                    .build()
                return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                    .setAvailableSessionCommands(updatedCommands)
                    .setAvailablePlayerCommands(base.availablePlayerCommands)
                    .build()
            }

            override fun onCustomCommand(
                session: MediaSession,
                controller: MediaSession.ControllerInfo,
                customCommand: SessionCommand,
                args: Bundle
            ): ListenableFuture<SessionResult> {
                return when (customCommand.customAction) {
                    EqualizerProtocol.COMMAND_APPLY_EQUALIZER -> {
                        val newSettings = EqualizerSettings.fromBundle(args)
                        equalizerProcessor.updateSettings(newSettings)
                        EqualizerStore.save(this@SonoraPlaybackService, newSettings)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    SleepTimerProtocol.COMMAND_SET_SLEEP_TIMER -> {
                        val durationMs = args.getLong(SleepTimerProtocol.KEY_DURATION_MS, 0L)
                        val fadeOut = args.getBoolean(SleepTimerProtocol.KEY_FADE_OUT, true)
                        scheduleServiceSleepTimer(session.player, durationMs, fadeOut)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    SleepTimerProtocol.COMMAND_CANCEL_SLEEP_TIMER -> {
                        cancelServiceSleepTimer(session.player)
                        Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                    }

                    else -> super.onCustomCommand(session, controller, customCommand, args)
                }
            }
        }

        mediaSession = MediaSession.Builder(this, player)
            .setCallback(callback)
            .build()
    }

    private fun scheduleServiceSleepTimer(
        player: Player,
        durationMs: Long,
        fadeOut: Boolean
    ) {
        sleepTimerJob?.cancel()
        player.volume = 1f
        if (durationMs <= 0L) return

        sleepTimerJob = serviceScope.launch {
            val fadeWindow = if (fadeOut) {
                SleepTimerProtocol.FADE_OUT_WINDOW_MS.coerceAtMost(durationMs)
            } else {
                0L
            }
            val waitBeforeFade = (durationMs - fadeWindow).coerceAtLeast(0L)
            if (waitBeforeFade > 0L) {
                delay(waitBeforeFade)
            }

            if (fadeWindow > 0L && player.isPlaying) {
                val steps = 15
                val stepDelay = (fadeWindow / steps).coerceAtLeast(40L)
                for (step in 1..steps) {
                    val fraction = 1f - (step.toFloat() / steps.toFloat())
                    player.volume = fraction.coerceIn(0.05f, 1f)
                    delay(stepDelay)
                }
            } else if (fadeWindow > 0L) {
                delay(fadeWindow)
            }

            player.pause()
            player.volume = 1f
        }
    }

    private fun cancelServiceSleepTimer(player: Player?) {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        player?.volume = 1f
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? =
        mediaSession

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0 ||
            player.playbackState == Player.STATE_ENDED
        ) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        sleepTimerJob?.cancel()
        serviceScope.cancel()
        mediaSession?.run {
            player.volume = 1f
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}
