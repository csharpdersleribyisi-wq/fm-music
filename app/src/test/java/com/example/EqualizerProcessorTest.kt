package com.example

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.util.UnstableApi
import com.example.player.EqualizerAudioProcessor
import com.example.player.EqualizerPreset
import com.example.player.EqualizerSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.sin
import kotlin.math.sqrt

@OptIn(UnstableApi::class)
class EqualizerProcessorTest {

    @Test
    fun bassBoostAmplifiesLowFrequencySineWave() {
        val processor = EqualizerAudioProcessor(
            EqualizerSettings(
                enabled = true,
                preset = EqualizerPreset.BASS_BOOST,
                bandGainsDb = EqualizerPreset.BASS_BOOST.bandGainsDb,
                preampDb = 0f
            )
        )
        val format = AudioProcessor.AudioFormat(44100, 1, C.ENCODING_PCM_16BIT)
        val configured = processor.configure(format)
        assertEquals(format, configured)
        processor.flush()

        val sampleCount = 2048
        val input = ByteBuffer.allocateDirect(sampleCount * 2).order(ByteOrder.LITTLE_ENDIAN)
        val rawSamples = ShortArray(sampleCount)
        for (i in 0 until sampleCount) {
            val s = (sin(2.0 * PI * 60.0 * i / 44100.0) * 6000.0).toInt().toShort()
            rawSamples[i] = s
            input.putShort(s)
        }
        input.flip()

        processor.queueInput(input)
        val output = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        assertEquals(sampleCount * 2, output.remaining())

        var inEnergy = 0.0
        var outEnergy = 0.0
        for (i in 0 until sampleCount) {
            val outSample = output.short.toDouble()
            // Skip initial filter transient when measuring steady-state RMS
            if (i >= sampleCount / 2) {
                val inSample = rawSamples[i].toDouble()
                inEnergy += inSample * inSample
                outEnergy += outSample * outSample
            }
        }
        val inRms = sqrt(inEnergy / (sampleCount / 2))
        val outRms = sqrt(outEnergy / (sampleCount / 2))
        assertTrue(
            "Expected Bass Boost (+7.5 dB @ 60 Hz) to raise RMS energy (in=$inRms, out=$outRms)",
            outRms > inRms * 1.4
        )
    }

    @Test
    fun disabledEqualizerActsAsBitExactBypass() {
        val processor = EqualizerAudioProcessor(
            EqualizerSettings(
                enabled = false,
                preset = EqualizerPreset.BASS_BOOST,
                bandGainsDb = EqualizerPreset.BASS_BOOST.bandGainsDb,
                preampDb = 4.0f
            )
        )
        val format = AudioProcessor.AudioFormat(48000, 2, C.ENCODING_PCM_16BIT)
        processor.configure(format)
        processor.flush()

        val sampleCount = 64
        val input = ByteBuffer.allocateDirect(sampleCount * 4).order(ByteOrder.LITTLE_ENDIAN)
        val expected = ShortArray(sampleCount * 2)
        for (i in expected.indices) {
            val v = ((i * 137) % 10000 - 5000).toShort()
            expected[i] = v
            input.putShort(v)
        }
        input.flip()

        processor.queueInput(input)
        val out = processor.output.order(ByteOrder.LITTLE_ENDIAN)
        for (i in expected.indices) {
            assertEquals(expected[i], out.short)
        }
    }

    @Test
    fun presetsAndModificationStatusWorkCorrectly() {
        val flat = EqualizerSettings()
        assertFalse(flat.isModifiedFromFlat)

        val jazz = EqualizerSettings(
            enabled = true,
            preset = EqualizerPreset.JAZZ,
            bandGainsDb = EqualizerPreset.JAZZ.bandGainsDb,
            preampDb = EqualizerPreset.JAZZ.preampDb
        )
        assertTrue(jazz.isModifiedFromFlat)
        assertEquals(5, jazz.normalizedGains().size)

        val disabledJazz = jazz.copy(enabled = false)
        assertFalse(disabledJazz.isModifiedFromFlat)
    }
}
