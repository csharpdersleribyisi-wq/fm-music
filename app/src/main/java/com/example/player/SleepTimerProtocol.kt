package com.example.player

import android.os.Bundle
import kotlin.math.ceil

object SleepTimerProtocol {
    const val COMMAND_SET_SLEEP_TIMER = "com.example.player.SET_SLEEP_TIMER"
    const val COMMAND_CANCEL_SLEEP_TIMER = "com.example.player.CANCEL_SLEEP_TIMER"
    const val KEY_DURATION_MS = "sleep_duration_ms"
    const val KEY_FADE_OUT = "sleep_fade_out"

    const val FADE_OUT_WINDOW_MS = 3_000L
    val PRESET_MINUTES = listOf(5, 15, 30, 45, 60, 90)

    fun buildSetBundle(durationMs: Long, fadeOut: Boolean): Bundle = Bundle().apply {
        putLong(KEY_DURATION_MS, durationMs.coerceAtLeast(0L))
        putBoolean(KEY_FADE_OUT, fadeOut)
    }

    fun formatCountdown(remainingMs: Long): String {
        val totalSeconds = ceil(remainingMs.coerceAtLeast(0L) / 1000.0).toLong()
        val hours = totalSeconds / 3600L
        val minutes = (totalSeconds % 3600L) / 60L
        val seconds = totalSeconds % 60L
        return if (hours > 0L) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%02d:%02d".format(minutes, seconds)
        }
    }

    fun formatDurationSummary(durationMs: Long): String {
        val totalMinutes = ((durationMs + 59_999L) / 60_000L).coerceAtLeast(1L)
        return if (totalMinutes >= 60L) {
            val h = totalMinutes / 60L
            val m = totalMinutes % 60L
            if (m == 0L) "${h}h" else "${h}h ${m}m"
        } else {
            "${totalMinutes} min"
        }
    }
}
