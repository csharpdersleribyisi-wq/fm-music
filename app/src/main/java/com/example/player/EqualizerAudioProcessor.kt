package com.example.player

import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.BaseAudioProcessor
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.math.tanh

/**
 * Real-time 5-band parametric/shelving equalizer implemented as a Media3 [BaseAudioProcessor].
 *
 * Hooks into ExoPlayer's [androidx.media3.exoplayer.audio.DefaultAudioSink] pipeline to apply
 * biquad IIR filtering (60 Hz low-shelf, 230 Hz / 910 Hz / 3.6 kHz peaking, 14 kHz high-shelf)
 * and preamp gain across decoded PCM 16-bit and float streams.
 */
@OptIn(UnstableApi::class)
class EqualizerAudioProcessor(
    initialSettings: EqualizerSettings = EqualizerSettings()
) : BaseAudioProcessor() {

    private class BiquadCoeffs(
        val b0: Double = 1.0,
        val b1: Double = 0.0,
        val b2: Double = 0.0,
        val a1: Double = 0.0,
        val a2: Double = 0.0
    )

    private class DspSnapshot(
        val enabled: Boolean,
        val preampLinear: Double,
        val bandActive: BooleanArray,
        val bands: Array<BiquadCoeffs>
    )

    @Volatile
    private var settings: EqualizerSettings = initialSettings

    @Volatile
    private var snapshot: DspSnapshot = buildSnapshot(
        settings = initialSettings,
        sampleRate = 44100
    )

    // Transposed Direct Form II delay lines per band x channel (up to 8 channels)
    private val z1 = Array(BAND_COUNT) { DoubleArray(MAX_CHANNELS) }
    private val z2 = Array(BAND_COUNT) { DoubleArray(MAX_CHANNELS) }

    fun updateSettings(newSettings: EqualizerSettings) {
        settings = newSettings
        val rate = inputAudioFormat.sampleRate.takeIf { it > 0 } ?: 44100
        snapshot = buildSnapshot(newSettings, rate)
    }

    fun currentSettings(): EqualizerSettings = settings

    @Throws(AudioProcessor.UnhandledAudioFormatException::class)
    override fun onConfigure(
        inputAudioFormat: AudioProcessor.AudioFormat
    ): AudioProcessor.AudioFormat {
        val encodingSupported = inputAudioFormat.encoding == C.ENCODING_PCM_16BIT ||
            inputAudioFormat.encoding == C.ENCODING_PCM_FLOAT
        val channelsSupported = inputAudioFormat.channelCount in 1..MAX_CHANNELS
        if (!encodingSupported || !channelsSupported || inputAudioFormat.sampleRate <= 0) {
            throw AudioProcessor.UnhandledAudioFormatException(inputAudioFormat)
        }
        snapshot = buildSnapshot(settings, inputAudioFormat.sampleRate)
        clearDelayLines()
        return inputAudioFormat
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        val remaining = inputBuffer.remaining()
        if (remaining == 0) return

        val outputBuffer = replaceOutputBuffer(remaining)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)

        val dsp = snapshot
        if (!dsp.enabled || (dsp.bandActive.none { it } && abs(dsp.preampLinear - 1.0) < 1e-4)) {
            outputBuffer.put(inputBuffer)
            outputBuffer.flip()
            return
        }

        val inView = inputBuffer.asReadOnlyBuffer().order(ByteOrder.LITTLE_ENDIAN)
        val channels = inputAudioFormat.channelCount.coerceIn(1, MAX_CHANNELS)
        val encoding = inputAudioFormat.encoding

        if (encoding == C.ENCODING_PCM_16BIT) {
            val frameBytes = channels * 2
            val frameCount = remaining / frameBytes
            for (frame in 0 until frameCount) {
                for (ch in 0 until channels) {
                    val sampleNorm = inView.short.toDouble() / 32768.0
                    val processed = processSample(sampleNorm, ch, dsp)
                    val outShort = (processed * 32767.0)
                        .roundToInt()
                        .coerceIn(-32768, 32767)
                        .toShort()
                    outputBuffer.putShort(outShort)
                }
            }
            // Advance inputBuffer position to match consumed bytes
            inputBuffer.position(inputBuffer.position() + frameCount * frameBytes)
        } else if (encoding == C.ENCODING_PCM_FLOAT) {
            val frameBytes = channels * 4
            val frameCount = remaining / frameBytes
            for (frame in 0 until frameCount) {
                for (ch in 0 until channels) {
                    val sampleNorm = inView.float.toDouble()
                    val processed = processSample(sampleNorm, ch, dsp)
                    outputBuffer.putFloat(processed.coerceIn(-1.0, 1.0).toFloat())
                }
            }
            inputBuffer.position(inputBuffer.position() + frameCount * frameBytes)
        }

        outputBuffer.flip()
    }

    override fun onFlush() {
        clearDelayLines()
    }

    override fun onReset() {
        clearDelayLines()
    }

    private fun processSample(
        inputSample: Double,
        channel: Int,
        dsp: DspSnapshot
    ): Double {
        var sample = inputSample * dsp.preampLinear
        for (bandIdx in 0 until BAND_COUNT) {
            if (!dsp.bandActive[bandIdx]) continue
            val c = dsp.bands[bandIdx]
            val bandZ1 = z1[bandIdx]
            val bandZ2 = z2[bandIdx]
            val y = c.b0 * sample + bandZ1[channel]
            bandZ1[channel] = c.b1 * sample - c.a1 * y + bandZ2[channel]
            bandZ2[channel] = c.b2 * sample - c.a2 * y
            sample = y
        }
        return softLimitSample(sample)
    }

    private fun softLimitSample(sample: Double): Double {
        val magnitude = abs(sample)
        if (magnitude <= LIMITER_THRESHOLD) return sample
        val headroom = 1.0 - LIMITER_THRESHOLD
        val compressed = LIMITER_THRESHOLD + headroom * tanh((magnitude - LIMITER_THRESHOLD) / headroom)
        return (sign(sample) * compressed).coerceIn(-1.0, 1.0)
    }

    private fun clearDelayLines() {
        for (band in 0 until BAND_COUNT) {
            z1[band].fill(0.0)
            z2[band].fill(0.0)
        }
    }

    private fun buildSnapshot(settings: EqualizerSettings, sampleRate: Int): DspSnapshot {
        val gains = settings.normalizedGains()
        val fs = sampleRate.coerceAtLeast(8000).toDouble()
        val bandActive = BooleanArray(BAND_COUNT)
        val bands = Array(BAND_COUNT) { idx ->
            val gainDb = gains[idx].toDouble()
            bandActive[idx] = abs(gainDb) >= 0.05
            if (!bandActive[idx]) {
                BiquadCoeffs()
            } else {
                val freq = min(EqualizerSettings.BAND_SPECS[idx].frequencyHz.toDouble(), fs * 0.45)
                when (idx) {
                    0 -> computeLowShelf(freq, fs, gainDb)
                    BAND_COUNT - 1 -> computeHighShelf(freq, fs, gainDb)
                    else -> computePeaking(freq, fs, gainDb, q = 1.2)
                }
            }
        }
        val preampLinear = 10.0.pow(
            settings.preampDb.coerceIn(
                EqualizerSettings.PREAMP_MIN_DB,
                EqualizerSettings.PREAMP_MAX_DB
            ) / 20.0
        )
        return DspSnapshot(
            enabled = settings.enabled,
            preampLinear = preampLinear,
            bandActive = bandActive,
            bands = bands
        )
    }

    private fun computePeaking(
        freq: Double,
        sampleRate: Double,
        gainDb: Double,
        q: Double
    ): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) / (2.0 * q)

        val b0 = 1.0 + alpha * a
        val b1 = -2.0 * cosW0
        val b2 = 1.0 - alpha * a
        val a0 = 1.0 + alpha / a
        val a1 = -2.0 * cosW0
        val a2 = 1.0 - alpha / a

        return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private fun computeLowShelf(
        freq: Double,
        sampleRate: Double,
        gainDb: Double
    ): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) * SQRT_HALF
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val b0 = a * ((a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha)
        val b1 = 2.0 * a * ((a - 1.0) - (a + 1.0) * cosW0)
        val b2 = a * ((a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha)
        val a0 = (a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha
        val a1 = -2.0 * ((a - 1.0) + (a + 1.0) * cosW0)
        val a2 = (a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha

        return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    private fun computeHighShelf(
        freq: Double,
        sampleRate: Double,
        gainDb: Double
    ): BiquadCoeffs {
        val a = 10.0.pow(gainDb / 40.0)
        val w0 = 2.0 * PI * freq / sampleRate
        val cosW0 = cos(w0)
        val alpha = sin(w0) * SQRT_HALF
        val twoSqrtAAlpha = 2.0 * sqrt(a) * alpha

        val b0 = a * ((a + 1.0) + (a - 1.0) * cosW0 + twoSqrtAAlpha)
        val b1 = -2.0 * a * ((a - 1.0) + (a + 1.0) * cosW0)
        val b2 = a * ((a + 1.0) + (a - 1.0) * cosW0 - twoSqrtAAlpha)
        val a0 = (a + 1.0) - (a - 1.0) * cosW0 + twoSqrtAAlpha
        val a1 = 2.0 * ((a - 1.0) - (a + 1.0) * cosW0)
        val a2 = (a + 1.0) - (a - 1.0) * cosW0 - twoSqrtAAlpha

        return BiquadCoeffs(b0 / a0, b1 / a0, b2 / a0, a1 / a0, a2 / a0)
    }

    companion object {
        private const val BAND_COUNT = 5
        private const val MAX_CHANNELS = 8
        private const val SQRT_HALF = 0.7071067811865476
        private const val LIMITER_THRESHOLD = 0.88
    }
}
