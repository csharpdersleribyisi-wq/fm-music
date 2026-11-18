package com.example.audio

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import java.io.ByteArrayOutputStream
import kotlin.math.sqrt

sealed interface LiveAudioEvent {
    data class Level(val rmsNormalized: Float) : LiveAudioEvent
    data class WavChunk(val wavBytes: ByteArray, val chunkIndex: Int, val durationMs: Long) : LiveAudioEvent
}

object LiveAudioRecorder {

    private const val TAG = "LiveAudioRecorder"
    private const val SAMPLE_RATE = 16_000
    private const val CHANNELS = 1
    private const val BITS_PER_SAMPLE = 16
    private const val CHUNK_SECONDS = 4

    @SuppressLint("MissingPermission")
    fun captureStream(): Flow<LiveAudioEvent> = flow {
        val minBuf = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        ).coerceAtLeast(SAMPLE_RATE / 2)

        val recorder = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            minBuf * 2
        )

        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw IllegalStateException("Microphone hardware failed to initialize AudioRecord.")
        }

        val targetPcmBytes = SAMPLE_RATE * CHANNELS * (BITS_PER_SAMPLE / 8) * CHUNK_SECONDS
        val pcmBuffer = ByteArrayOutputStream(targetPcmBytes)
        val readBuf = ShortArray(1024)
        var chunkIndex = 1

        try {
            recorder.startRecording()
            while (currentCoroutineContext().isActive) {
                val readCount = recorder.read(readBuf, 0, readBuf.size)
                if (readCount > 0) {
                    var sumSquares = 0.0
                    for (i in 0 until readCount) {
                        val s = readBuf[i]
                        sumSquares += s.toDouble() * s.toDouble()
                        pcmBuffer.write(s.toInt() and 0xFF)
                        pcmBuffer.write((s.toInt() shr 8) and 0xFF)
                    }
                    val rms = sqrt(sumSquares / readCount) / 14000.0
                    emit(LiveAudioEvent.Level(rms.toFloat().coerceIn(0.04f, 1f)))

                    if (pcmBuffer.size() >= targetPcmBytes) {
                        val pcm = pcmBuffer.toByteArray()
                        pcmBuffer.reset()
                        val wav = wrapPcmAsWav(pcm, SAMPLE_RATE, CHANNELS, BITS_PER_SAMPLE)
                        emit(
                            LiveAudioEvent.WavChunk(
                                wavBytes = wav,
                                chunkIndex = chunkIndex++,
                                durationMs = CHUNK_SECONDS * 1000L
                            )
                        )
                    }
                } else if (readCount < 0) {
                    Log.w(TAG, "AudioRecord read error code: $readCount")
                    break
                }
            }
        } finally {
            runCatching {
                if (recorder.recordingState == AudioRecord.RECORDSTATE_RECORDING) {
                    recorder.stop()
                }
            }
            recorder.release()
        }
    }.flowOn(Dispatchers.IO)

    fun wrapPcmAsWav(
        pcmData: ByteArray,
        sampleRate: Int,
        channels: Int,
        bitsPerSample: Int
    ): ByteArray {
        val totalDataLen = pcmData.size + 36
        val byteRate = sampleRate * channels * bitsPerSample / 8
        val header = ByteArray(44)

        header[0] = 'R'.code.toByte()
        header[1] = 'I'.code.toByte()
        header[2] = 'F'.code.toByte()
        header[3] = 'F'.code.toByte()
        writeIntLE(header, 4, totalDataLen)
        header[8] = 'W'.code.toByte()
        header[9] = 'A'.code.toByte()
        header[10] = 'V'.code.toByte()
        header[11] = 'E'.code.toByte()
        header[12] = 'f'.code.toByte()
        header[13] = 'm'.code.toByte()
        header[14] = 't'.code.toByte()
        header[15] = ' '.code.toByte()
        writeIntLE(header, 16, 16)
        writeShortLE(header, 20, 1)
        writeShortLE(header, 22, channels.toShort())
        writeIntLE(header, 24, sampleRate)
        writeIntLE(header, 28, byteRate)
        writeShortLE(header, 32, (channels * bitsPerSample / 8).toShort())
        writeShortLE(header, 34, bitsPerSample.toShort())
        header[36] = 'd'.code.toByte()
        header[37] = 'a'.code.toByte()
        header[38] = 't'.code.toByte()
        header[39] = 'a'.code.toByte()
        writeIntLE(header, 40, pcmData.size)

        return ByteArray(44 + pcmData.size).also { out ->
            System.arraycopy(header, 0, out, 0, 44)
            System.arraycopy(pcmData, 0, out, 44, pcmData.size)
        }
    }

    private fun writeIntLE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset] = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun writeShortLE(buf: ByteArray, offset: Int, value: Short) {
        val v = value.toInt()
        buf[offset] = (v and 0xFF).toByte()
        buf[offset + 1] = ((v shr 8) and 0xFF).toByte()
    }
}
