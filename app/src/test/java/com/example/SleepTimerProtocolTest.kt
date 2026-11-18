package com.example

import com.example.player.SleepTimerProtocol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SleepTimerProtocolTest {

    @Test
    fun formatCountdownFormatsMinutesSecondsAndHoursAccurately() {
        assertEquals("00:00", SleepTimerProtocol.formatCountdown(0L))
        assertEquals("00:00", SleepTimerProtocol.formatCountdown(-500L))
        assertEquals("00:01", SleepTimerProtocol.formatCountdown(1L))
        assertEquals("01:05", SleepTimerProtocol.formatCountdown(65_000L))
        assertEquals("15:00", SleepTimerProtocol.formatCountdown(15 * 60_000L))
        assertEquals("1:01:01", SleepTimerProtocol.formatCountdown(3_661_000L))
    }

    @Test
    fun formatDurationSummaryFormatsShortAndLongDurations() {
        assertEquals("1 min", SleepTimerProtocol.formatDurationSummary(0L))
        assertEquals("1 min", SleepTimerProtocol.formatDurationSummary(30_000L))
        assertEquals("15 min", SleepTimerProtocol.formatDurationSummary(15 * 60_000L))
        assertEquals("1h", SleepTimerProtocol.formatDurationSummary(60 * 60_000L))
        assertEquals("1h 30m", SleepTimerProtocol.formatDurationSummary(90 * 60_000L))
    }

    @Test
    fun buildSetBundleClampsNegativeDurationAndPreservesFadeOutFlag() {
        val positiveBundle = SleepTimerProtocol.buildSetBundle(300_000L, fadeOut = true)
        assertEquals(300_000L, positiveBundle.getLong(SleepTimerProtocol.KEY_DURATION_MS))
        assertTrue(positiveBundle.getBoolean(SleepTimerProtocol.KEY_FADE_OUT))

        val clampedBundle = SleepTimerProtocol.buildSetBundle(-1_000L, fadeOut = false)
        assertEquals(0L, clampedBundle.getLong(SleepTimerProtocol.KEY_DURATION_MS))
        assertFalse(clampedBundle.getBoolean(SleepTimerProtocol.KEY_FADE_OUT))
    }

    @Test
    fun presetMinutesContainExpectedSortedDurations() {
        assertEquals(listOf(5, 15, 30, 45, 60, 90), SleepTimerProtocol.PRESET_MINUTES)
    }
}
