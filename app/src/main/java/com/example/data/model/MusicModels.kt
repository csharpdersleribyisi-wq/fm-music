package com.example.data.model

import androidx.annotation.Keep
import androidx.annotation.RawRes
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

data class Track(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val genre: String,
    val mood: String,
    val durationMs: Long,
    val bpm: Int,
    @RawRes val rawResId: Int,
    val coverKey: String,
    val accentHex: Long,
    val bitrateLabel: String = "352 kbps WAV"
)

data class AlbumInfo(
    val title: String,
    val artist: String,
    val genre: String,
    val coverKey: String,
    val accentHex: Long,
    val tracks: List<Track>
)

@Keep
@IgnoreExtraProperties
data class UserTasteProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val username: String = "",
    val favoriteGenres: List<String> = emptyList(),
    val preferredMood: String = "Focus",
    val offlineModeEnabled: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserDirectoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val username: String = "",
    val displayName: String = "",
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserPlaylist(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val description: String = "",
    val moodTag: String = "Personal Mix",
    val coverKey: String = "midnight_synth",
    val trackIds: List<String> = emptyList(),
    val personalized: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CollaborativePlaylist(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val title: String = "",
    val description: String = "",
    val moodTag: String = "Shared Mix",
    val coverKey: String = "sonic_horizon",
    val trackIds: List<String> = emptyList(),
    val collaboratorUids: List<String> = emptyList(),
    val invitedHandles: List<String> = emptyList(),
    val inviteCode: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PublicPlaylist(
    @DocumentId val id: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    val ownerUsername: String = "",
    val sourcePlaylistId: String = "",
    val title: String = "",
    val description: String = "",
    val moodTag: String = "Public Mix",
    val coverKey: String = "midnight_synth",
    val trackIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CollabInvite(
    @DocumentId val id: String = "",
    val inviteCode: String = "",
    val playlistId: String = "",
    val title: String = "",
    val ownerId: String = "",
    val ownerName: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class LikedTrack(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val genre: String = "",
    @ServerTimestamp val likedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PlayHistoryEntry(
    @DocumentId val id: String = "",
    val userId: String = "",
    val trackId: String = "",
    val genre: String = "",
    val mood: String = "",
    @ServerTimestamp val playedAt: Timestamp? = null
)

data class PersonalizedMix(
    val id: String,
    val title: String,
    val subtitle: String,
    val reason: String,
    val moodTag: String,
    val coverKey: String,
    val tracks: List<Track>,
    val matchPercent: Int
)

data class RecommendedTrack(
    val track: Track,
    val matchPercent: Int,
    val reason: String,
    val badges: List<String>
)

data class ListeningHabitSummary(
    val topGenres: List<Pair<String, Int>>,
    val topArtists: List<Pair<String, Int>>,
    val mostPlayedTracks: List<Pair<Track, Int>>,
    val likedCount: Int,
    val playlistTrackCount: Int,
    val totalPlays: Int
)

data class TranscriptSegment(
    val id: String,
    val sourceLabel: String,
    val timestampLabel: String,
    val text: String,
    val streaming: Boolean = false
)

enum class OfflineDownloadStatus {
    NONE,
    DOWNLOADING,
    DOWNLOADED,
    FAILED
}
