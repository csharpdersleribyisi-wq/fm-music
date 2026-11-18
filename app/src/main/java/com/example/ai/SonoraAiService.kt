package com.example.ai

import android.content.Context
import android.util.Log
import com.example.data.catalog.MusicCatalog
import com.example.data.model.Track
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.content
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiRecommendedPick(
    val trackId: String,
    val reason: String
)

data class AiForYouInsights(
    val headline: String,
    val tasteSummary: String,
    val picks: List<AiRecommendedPick>
)

class SonoraAiService(private val appContext: Context) {

    private val insightsSchema = Schema.obj(
        properties = mapOf(
            "headline" to Schema.string("Short vibrant 4-7 word personalized headline"),
            "tasteSummary" to Schema.string("2-sentence analysis of the listener's genres, artists, favorites, and playlist habits"),
            "picks" to Schema.array(
                items = Schema.obj(
                    properties = mapOf(
                        "trackId" to Schema.string("Exact track ID from the provided catalog"),
                        "reason" to Schema.string("Specific 1-sentence reason linking this track to the listener's habits")
                    )
                ),
                description = "4 recommended track picks prioritized toward fresh tracks matching user taste"
            )
        )
    )

    private val insightsModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = insightsSchema
            }
        )
    }

    private val transcriptionModel by lazy {
        Firebase.ai.generativeModel(modelName = MODEL_NAME)
    }

    suspend fun generateForYouInsights(
        favoriteGenres: List<String>,
        preferredMood: String,
        topPlayedTracks: List<Pair<Track, Int>>,
        likedTracks: List<Track>,
        playlistTracks: List<Track>
    ): AiForYouInsights {
        val catalogLines = MusicCatalog.tracks.joinToString("\n") { track ->
            "- id=${track.id} | title=${track.title} | artist=${track.artist} | album=${track.album} | genre=${track.genre} | mood=${track.mood} | bpm=${track.bpm}"
        }
        val playSummary = if (topPlayedTracks.isEmpty()) {
            "No plays recorded yet"
        } else {
            topPlayedTracks.joinToString(", ") { "${it.first.title} by ${it.first.artist} (${it.second}x)" }
        }
        val likedSummary = if (likedTracks.isEmpty()) {
            "None yet"
        } else {
            likedTracks.joinToString(", ") { "${it.title} (${it.artist}, ${it.genre})" }
        }
        val playlistSummary = if (playlistTracks.isEmpty()) {
            "No saved playlist tracks yet"
        } else {
            playlistTracks.distinctBy { it.id }.joinToString(", ") { "${it.title} (${it.artist}, ${it.genre})" }
        }

        val prompt = """
            You are Sonora's music taste analyst. Analyze this listener's music habits and recommend 4 tracks from the catalog below.
            Listener preferences:
            - Favorite Genres: ${favoriteGenres.joinToString(", ").ifBlank { "Open" }}
            - Preferred Mood: $preferredMood
            - Frequently Played: $playSummary
            - Liked Songs: $likedSummary
            - Current Playlist Tracks: $playlistSummary

            Full Catalog:
            $catalogLines

            Prioritize tracks that expand on the listener's genres, artists, moods, and playlist content (favoring unplayed or un-saved discoveries when available).
            Return exact catalog track IDs in picks.
        """.trimIndent()

        return try {
            val response = withTimeout(TIMEOUT_MS) {
                insightsModel.generateContent(prompt)
            }
            val rawJson = response.text ?: throw IllegalStateException("AI insights returned empty response.")
            val json = JSONObject(rawJson)
            val headline = json.optString("headline", "Your Sonic Taste Radar")
            val summary = json.optString("tasteSummary", "Curated from your listening history and saved playlists.")
            val picksJson = json.optJSONArray("picks")
            val picks = buildList {
                if (picksJson != null) {
                    for (i in 0 until picksJson.length()) {
                        val item = picksJson.optJSONObject(i) ?: continue
                        val tid = item.optString("trackId")
                        val reason = item.optString("reason")
                        if (MusicCatalog.getTrack(tid) != null && reason.isNotBlank()) {
                            add(AiRecommendedPick(tid, reason))
                        }
                    }
                }
            }
            AiForYouInsights(headline = headline, tasteSummary = summary, picks = picks)
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "For You insights timed out", e)
            throw IllegalStateException("AI taste analysis timed out after 60 seconds. Please retry.", e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Failed generating For You insights", e)
            throw e
        }
    }

    fun transcribeAudioBytesStream(
        wavBytes: ByteArray,
        feedLabel: String,
        isAmbientMic: Boolean
    ): Flow<String> = flow {
        require(wavBytes.isNotEmpty()) { "Audio buffer is empty." }
        require(wavBytes.size < MAX_INLINE_AUDIO_BYTES) {
            "Audio feed chunk exceeds 20 MB inline limit."
        }
        val instruction = if (isAmbientMic) {
            """
            Transcribe all spoken words, vocals, lyrics, or audible cues from this live microphone audio feed ($feedLabel) in real time.
            - If there is clear speech or singing, transcribe it verbatim.
            - If there is instrumental music, describe the dominant motif/instrument/tempo concisely in brackets like [Ambient synth chords, ~90 BPM].
            - If the clip is quiet/room tone, respond briefly with [Silence / ambient room tone].
            Output plain transcript text only without markdown.
            """.trimIndent()
        } else {
            """
            Listen to this audio track feed ($feedLabel) and produce a live real-time audio transcription and sonic captioning readout.
            - Transcribe any vocals, spoken cues, or melodic phrases detected.
            - Annotate instrument entries, rhythm/BPM texture, and harmonic shifts in concise timed brackets like [00:00] ...
            Keep it concise, accurate, and readable as a live feed caption.
            """.trimIndent()
        }

        withTimeout(TIMEOUT_MS) {
            transcriptionModel.generateContentStream(
                content {
                    inlineData(wavBytes, "audio/wav")
                    text(instruction)
                }
            ).collect { chunk ->
                val text = chunk.text.orEmpty()
                if (text.isNotEmpty()) {
                    emit(text)
                }
            }
        }
    }.flowOn(Dispatchers.IO)

    suspend fun loadTrackWavBytes(track: Track): ByteArray = withContext(Dispatchers.IO) {
        appContext.resources.openRawResource(track.rawResId).use { input ->
            input.readBytes()
        }
    }

    private companion object {
        const val TAG = "SonoraAiService"
        const val MODEL_NAME = "gemini-3.8-flash"
        const val TIMEOUT_MS = 60_000L
        const val MAX_INLINE_AUDIO_BYTES = 19 * 1024 * 1024
    }
}
