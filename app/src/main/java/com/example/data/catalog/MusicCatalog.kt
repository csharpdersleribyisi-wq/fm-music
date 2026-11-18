package com.example.data.catalog

import com.example.R
import com.example.data.model.AlbumInfo
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.ListeningHabitSummary
import com.example.data.model.PersonalizedMix
import com.example.data.model.PlayHistoryEntry
import com.example.data.model.RecommendedTrack
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.data.model.UserTasteProfile

object MusicCatalog {

    val allGenres = listOf(
        "Synthwave",
        "Lo-Fi Chill",
        "Acoustic Dawn",
        "Deep Focus",
        "Late Night Jazz",
        "Indie Pulse"
    )

    val allMoods = listOf(
        "Energetic",
        "Midnight",
        "Relaxed",
        "Focus",
        "Uplifting",
        "Warm"
    )

    val coverKeys = listOf(
        "midnight_synth",
        "velvet_lofi",
        "acoustic_dawn",
        "sonic_horizon"
    )

    val tracks: List<Track> = listOf(
        Track(
            id = "track_neon_pulse",
            title = "Neon Pulse",
            artist = "Kairo Vance",
            album = "Afterglow Circuits",
            genre = "Synthwave",
            mood = "Energetic",
            durationMs = 20_000L,
            bpm = 118,
            rawResId = R.raw.track_neon_pulse,
            coverKey = "midnight_synth",
            accentHex = 0xFF1ED760
        ),
        Track(
            id = "track_chrome_horizon",
            title = "Chrome Horizon",
            artist = "Mira Solis",
            album = "Afterglow Circuits",
            genre = "Synthwave",
            mood = "Midnight",
            durationMs = 20_000L,
            bpm = 110,
            rawResId = R.raw.track_chrome_horizon,
            coverKey = "sonic_horizon",
            accentHex = 0xFF00E5C3
        ),
        Track(
            id = "track_velvet_rain",
            title = "Velvet Rain",
            artist = "Nori Beats",
            album = "Rainy Turntable",
            genre = "Lo-Fi Chill",
            mood = "Relaxed",
            durationMs = 20_000L,
            bpm = 78,
            rawResId = R.raw.track_velvet_rain,
            coverKey = "velvet_lofi",
            accentHex = 0xFFFFB347
        ),
        Track(
            id = "track_coffeehouse_tape",
            title = "Coffeehouse Tape",
            artist = "Juno Park",
            album = "Rainy Turntable",
            genre = "Lo-Fi Chill",
            mood = "Focus",
            durationMs = 20_000L,
            bpm = 82,
            rawResId = R.raw.track_coffeehouse_tape,
            coverKey = "velvet_lofi",
            accentHex = 0xFFFF9F43
        ),
        Track(
            id = "track_golden_meadow",
            title = "Golden Meadow",
            artist = "Elena Rivers",
            album = "First Light",
            genre = "Acoustic Dawn",
            mood = "Uplifting",
            durationMs = 20_000L,
            bpm = 92,
            rawResId = R.raw.track_golden_meadow,
            coverKey = "acoustic_dawn",
            accentHex = 0xFF4CD4B0
        ),
        Track(
            id = "track_amber_strings",
            title = "Amber Strings",
            artist = "Theo Arden",
            album = "First Light",
            genre = "Acoustic Dawn",
            mood = "Warm",
            durationMs = 20_000L,
            bpm = 86,
            rawResId = R.raw.track_amber_strings,
            coverKey = "acoustic_dawn",
            accentHex = 0xFFE6B86A
        ),
        Track(
            id = "track_orbital_drift",
            title = "Orbital Drift",
            artist = "Astral Form",
            album = "Quiet Telemetry",
            genre = "Deep Focus",
            mood = "Focus",
            durationMs = 20_000L,
            bpm = 70,
            rawResId = R.raw.track_orbital_drift,
            coverKey = "sonic_horizon",
            accentHex = 0xFF3FD8FF
        ),
        Track(
            id = "track_submerged_glass",
            title = "Submerged Glass",
            artist = "Lumen Echo",
            album = "Quiet Telemetry",
            genre = "Deep Focus",
            mood = "Relaxed",
            durationMs = 20_000L,
            bpm = 66,
            rawResId = R.raw.track_submerged_glass,
            coverKey = "sonic_horizon",
            accentHex = 0xFF6B8AFF
        ),
        Track(
            id = "track_midnight_espresso",
            title = "Midnight Espresso",
            artist = "Miles K",
            album = "Velvet Brass",
            genre = "Late Night Jazz",
            mood = "Midnight",
            durationMs = 20_000L,
            bpm = 88,
            rawResId = R.raw.track_midnight_espresso,
            coverKey = "velvet_lofi",
            accentHex = 0xFFFFB347
        ),
        Track(
            id = "track_blue_velvet_sax",
            title = "Blue Velvet Suite",
            artist = "Clara Nox",
            album = "Velvet Brass",
            genre = "Late Night Jazz",
            mood = "Warm",
            durationMs = 20_000L,
            bpm = 76,
            rawResId = R.raw.track_blue_velvet_sax,
            coverKey = "velvet_lofi",
            accentHex = 0xFF8B7BFF
        ),
        Track(
            id = "track_electric_bloom",
            title = "Electric Bloom",
            artist = "Vespera",
            album = "Prism Motion",
            genre = "Indie Pulse",
            mood = "Energetic",
            durationMs = 20_000L,
            bpm = 122,
            rawResId = R.raw.track_electric_bloom,
            coverKey = "midnight_synth",
            accentHex = 0xFF1ED760
        ),
        Track(
            id = "track_aurora_drive",
            title = "Aurora Drive",
            artist = "Soren & The Waves",
            album = "Prism Motion",
            genre = "Indie Pulse",
            mood = "Uplifting",
            durationMs = 20_000L,
            bpm = 116,
            rawResId = R.raw.track_aurora_drive,
            coverKey = "acoustic_dawn",
            accentHex = 0xFF00E5C3
        )
    )

    private val trackById = tracks.associateBy { it.id }

    fun getTrack(id: String): Track? = trackById[id]

    fun resolveTracks(ids: List<String>): List<Track> = ids.mapNotNull { trackById[it] }

    fun getAlbums(): List<AlbumInfo> = tracks
        .groupBy { it.album }
        .map { (albumTitle, albumTracks) ->
            val first = albumTracks.first()
            val artistLabel = albumTracks.map { it.artist }.distinct().joinToString(" • ")
            AlbumInfo(
                title = albumTitle,
                artist = artistLabel,
                genre = first.genre,
                coverKey = first.coverKey,
                accentHex = first.accentHex,
                tracks = albumTracks
            )
        }
}

object PersonalizationEngine {

    fun analyzeListeningHabits(
        profile: UserTasteProfile?,
        playlists: List<UserPlaylist>,
        collaborativePlaylists: List<CollaborativePlaylist>,
        likedTrackIds: Set<String>,
        playHistory: List<PlayHistoryEntry>
    ): ListeningHabitSummary {
        val playCounts = playHistory.groupingBy { it.trackId }.eachCount()
        val playlistTrackIds = (playlists.flatMap { it.trackIds } +
            collaborativePlaylists.flatMap { it.trackIds }).toSet()

        val genreScores = mutableMapOf<String, Int>()
        val artistScores = mutableMapOf<String, Int>()

        profile?.favoriteGenres?.forEach { genre ->
            genreScores[genre] = (genreScores[genre] ?: 0) + 3
        }
        playHistory.forEach { entry ->
            val track = MusicCatalog.getTrack(entry.trackId)
            genreScores[entry.genre] = (genreScores[entry.genre] ?: 0) + 2
            if (track != null) {
                artistScores[track.artist] = (artistScores[track.artist] ?: 0) + 2
            }
        }
        likedTrackIds.forEach { tid ->
            MusicCatalog.getTrack(tid)?.let { track ->
                genreScores[track.genre] = (genreScores[track.genre] ?: 0) + 4
                artistScores[track.artist] = (artistScores[track.artist] ?: 0) + 3
            }
        }
        playlistTrackIds.forEach { tid ->
            MusicCatalog.getTrack(tid)?.let { track ->
                genreScores[track.genre] = (genreScores[track.genre] ?: 0) + 2
                artistScores[track.artist] = (artistScores[track.artist] ?: 0) + 2
            }
        }

        val mostPlayed = playCounts.entries
            .mapNotNull { (tid, count) -> MusicCatalog.getTrack(tid)?.let { it to count } }
            .sortedByDescending { it.second }
            .take(5)

        return ListeningHabitSummary(
            topGenres = genreScores.entries.sortedByDescending { it.value }.map { it.key to it.value }.take(4),
            topArtists = artistScores.entries.sortedByDescending { it.value }.map { it.key to it.value }.take(4),
            mostPlayedTracks = mostPlayed,
            likedCount = likedTrackIds.size,
            playlistTrackCount = playlistTrackIds.size,
            totalPlays = playHistory.size
        )
    }

    fun generateForYouRecommendations(
        profile: UserTasteProfile?,
        playlists: List<UserPlaylist>,
        collaborativePlaylists: List<CollaborativePlaylist>,
        likedTrackIds: Set<String>,
        playHistory: List<PlayHistoryEntry>
    ): List<RecommendedTrack> {
        val preferredGenres = profile?.favoriteGenres?.toSet().orEmpty()
        val preferredMood = profile?.preferredMood ?: "Focus"
        val playCounts = playHistory.groupingBy { it.trackId }.eachCount()
        val playlistTrackIds = (playlists.flatMap { it.trackIds } +
            collaborativePlaylists.flatMap { it.trackIds }).toSet()

        val likedTracks = likedTrackIds.mapNotNull { MusicCatalog.getTrack(it) }
        val playlistTracks = playlistTrackIds.mapNotNull { MusicCatalog.getTrack(it) }
        val playedTracks = playHistory.mapNotNull { MusicCatalog.getTrack(it.trackId) }

        val affinityGenres = buildMap<String, Int> {
            preferredGenres.forEach { put(it, (get(it) ?: 0) + 28) }
            likedTracks.forEach { put(it.genre, (get(it.genre) ?: 0) + 18) }
            playlistTracks.forEach { put(it.genre, (get(it.genre) ?: 0) + 14) }
            playedTracks.forEach { put(it.genre, (get(it.genre) ?: 0) + 9) }
        }

        val affinityArtists = buildMap<String, Int> {
            likedTracks.forEach { put(it.artist, (get(it.artist) ?: 0) + 22) }
            playlistTracks.forEach { put(it.artist, (get(it.artist) ?: 0) + 16) }
            playedTracks.forEach { put(it.artist, (get(it.artist) ?: 0) + 10) }
        }

        val affinityAlbums = buildMap<String, Int> {
            likedTracks.forEach { put(it.album, (get(it.album) ?: 0) + 12) }
            playlistTracks.forEach { put(it.album, (get(it.album) ?: 0) + 10) }
        }

        val avgLikedBpm = (likedTracks + playedTracks)
            .map { it.bpm }
            .takeIf { it.isNotEmpty() }
            ?.average()

        return MusicCatalog.tracks.map { track ->
            var score = 42
            val reasons = mutableListOf<String>()
            val badges = mutableListOf<String>()

            val genreWeight = affinityGenres[track.genre] ?: 0
            if (genreWeight > 0) {
                score += (genreWeight / 2).coerceAtMost(24)
                reasons += "Matches your ${track.genre} affinity"
                badges += track.genre
            }

            val artistWeight = affinityArtists[track.artist] ?: 0
            if (artistWeight > 0) {
                score += (artistWeight / 2).coerceAtMost(18)
                reasons += "Echoes ${track.artist} in your rotation"
            }

            if (track.mood.equals(preferredMood, ignoreCase = true)) {
                score += 14
                reasons += "Fits your $preferredMood mood"
                badges += track.mood
            }

            if ((affinityAlbums[track.album] ?: 0) > 0 && track.id !in likedTrackIds) {
                score += 10
                reasons += "Companion cut from ${track.album}"
            }

            if (avgLikedBpm != null && kotlin.math.abs(track.bpm - avgLikedBpm) <= 14.0) {
                score += 8
            }

            val plays = playCounts[track.id] ?: 0
            val isNewDiscovery = track.id !in likedTrackIds && track.id !in playlistTrackIds && plays == 0
            if (isNewDiscovery) {
                score += 12
                badges += "New Discovery"
            } else if (plays >= 2) {
                score += 6
                badges += "Heavy Rotation"
            }

            if (track.id in playlistTrackIds && track.id !in likedTrackIds) {
                badges += "In Playlist"
            }

            val finalReason = if (reasons.isNotEmpty()) {
                reasons.distinct().take(2).joinToString(" • ")
            } else {
                "Fresh ${track.genre} texture (${track.bpm} BPM) curated to broaden your mix"
            }

            RecommendedTrack(
                track = track,
                matchPercent = score.coerceIn(68, 99),
                reason = finalReason,
                badges = badges.distinct().take(3)
            )
        }.sortedWith(
            compareByDescending<RecommendedTrack> {
                if (it.track.id !in likedTrackIds && it.track.id !in playlistTrackIds) 1 else 0
            }.thenByDescending { it.matchPercent }
        )
    }

    fun generatePersonalizedMixes(
        profile: UserTasteProfile?,
        likedTrackIds: Set<String>,
        playHistory: List<PlayHistoryEntry>
    ): List<PersonalizedMix> {
        val preferredGenres = profile?.favoriteGenres?.toSet().orEmpty()
        val preferredMood = profile?.preferredMood ?: "Focus"
        val playCounts = playHistory.groupingBy { it.trackId }.eachCount()
        val genrePlayCounts = playHistory.groupingBy { it.genre }.eachCount()

        val scored = MusicCatalog.tracks.map { track ->
            var score = 10
            if (track.genre in preferredGenres) score += 28
            if (track.mood.equals(preferredMood, ignoreCase = true)) score += 22
            if (track.id in likedTrackIds) score += 30
            score += (playCounts[track.id] ?: 0) * 12
            score += (genrePlayCounts[track.genre] ?: 0) * 5
            track to score
        }.sortedByDescending { it.second }

        val topGenre = preferredGenres.firstOrNull()
            ?: genrePlayCounts.maxByOrNull { it.value }?.key
            ?: "Synthwave"

        val dailyMix1Tracks = scored.take(5).map { it.first }
        val focusTracks = MusicCatalog.tracks
            .sortedByDescending {
                (if (it.mood == "Focus" || it.genre == "Deep Focus" || it.genre == "Lo-Fi Chill") 40 else 0) +
                    (if (it.id in likedTrackIds) 15 else 0)
            }
            .take(4)

        val nightTracks = MusicCatalog.tracks
            .sortedByDescending {
                (if (it.mood in listOf("Midnight", "Relaxed", "Warm")) 40 else 0) +
                    (if (it.genre in listOf("Synthwave", "Late Night Jazz", "Lo-Fi Chill")) 20 else 0)
            }
            .take(5)

        val energyTracks = MusicCatalog.tracks
            .sortedByDescending {
                (if (it.bpm >= 92) 35 else 0) +
                    (if (it.mood in listOf("Energetic", "Uplifting")) 25 else 0)
            }
            .take(4)

        val topScore = (scored.firstOrNull()?.second ?: 40).coerceIn(72, 99)

        return listOf(
            PersonalizedMix(
                id = "mix_daily_horizon",
                title = "Your Daily Sonic Mix",
                subtitle = "Tailored to $topGenre & $preferredMood vibes",
                reason = if (likedTrackIds.isNotEmpty()) {
                    "Boosted by ${likedTrackIds.size} liked tracks"
                } else {
                    "Curated from your taste profile"
                },
                moodTag = preferredMood,
                coverKey = "sonic_horizon",
                tracks = dailyMix1Tracks,
                matchPercent = topScore
            ),
            PersonalizedMix(
                id = "mix_deep_focus",
                title = "Deep Focus Flow",
                subtitle = "Low-distraction textures & steady warmth",
                reason = "Ideal for work, study & offline sessions",
                moodTag = "Focus",
                coverKey = "velvet_lofi",
                tracks = focusTracks,
                matchPercent = 94
            ),
            PersonalizedMix(
                id = "mix_midnight_drive",
                title = "Midnight Afterglow",
                subtitle = "Analog synths, smooth brass & late-night tape",
                reason = "Matches evening listening cadence",
                moodTag = "Midnight",
                coverKey = "midnight_synth",
                tracks = nightTracks,
                matchPercent = 91
            ),
            PersonalizedMix(
                id = "mix_uplift_pulse",
                title = "Morning Momentum",
                subtitle = "Crisp acoustic dawns & high-tempo indie",
                reason = "Higher BPM tracks to lift your energy",
                moodTag = "Energetic",
                coverKey = "acoustic_dawn",
                tracks = energyTracks,
                matchPercent = 88
            )
        )
    }
}
