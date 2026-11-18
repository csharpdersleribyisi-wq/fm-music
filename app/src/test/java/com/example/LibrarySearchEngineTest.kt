package com.example

import com.example.data.catalog.MusicCatalog
import com.example.data.local.OfflineTrackEntity
import com.example.data.model.ListeningHabitSummary
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.UserPlaylist
import com.example.ui.LibraryFilter
import com.example.ui.SonoraHomeData
import com.example.ui.screens.LibrarySearchEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibrarySearchEngineTest {

    private fun sampleHomeData(): SonoraHomeData {
        val neon = MusicCatalog.getTrack("track_neon_pulse")!!
        val coffee = MusicCatalog.getTrack("track_coffeehouse_tape")!!
        return SonoraHomeData(
            profile = null,
            playlists = listOf(
                UserPlaylist(
                    id = "pl_1",
                    userId = "u1",
                    title = "Late Coding Session",
                    moodTag = "Focus",
                    trackIds = listOf(neon.id, coffee.id)
                )
            ),
            collaborativePlaylists = emptyList(),
            publicPlaylists = emptyList(),
            likedTrackIds = setOf(neon.id),
            playHistory = emptyList(),
            personalizedMixes = emptyList(),
            forYouRecommendations = emptyList(),
            habitSummary = ListeningHabitSummary(
                topGenres = emptyList(),
                topArtists = emptyList(),
                mostPlayedTracks = emptyList(),
                likedCount = 1,
                playlistTrackCount = 2,
                totalPlays = 0
            ),
            offlineMap = mapOf(
                coffee.id to OfflineTrackEntity(
                    trackId = coffee.id,
                    localFilePath = "/tmp/coffee.wav",
                    fileSizeBytes = 1024L,
                    statusName = OfflineDownloadStatus.DOWNLOADED.name,
                    progressPercent = 100,
                    downloadedAt = 1000L
                )
            )
        )
    }

    @Test
    fun searchFindsTracksArtistsAndAlbumsByName() {
        val data = sampleHomeData()

        // Find track by title
        val byTitle = LibrarySearchEngine.search("Neon Pulse", LibraryFilter.ALL, data)
        assertFalse(byTitle.isEmpty)
        assertEquals(listOf("track_neon_pulse"), byTitle.tracks.map { it.id })

        // Find artist by name
        val byArtist = LibrarySearchEngine.search("Kairo", LibraryFilter.ALL, data)
        assertEquals(listOf("Kairo Vance"), byArtist.artists.map { it.artist })
        assertEquals(1, byArtist.tracks.size)

        // Find album by name
        val byAlbum = LibrarySearchEngine.search("Afterglow", LibraryFilter.ALL, data)
        assertEquals(listOf("Afterglow Circuits"), byAlbum.albums.map { it.title })
        assertEquals(2, byAlbum.tracks.size)
    }

    @Test
    fun searchRespectsActiveLibraryFilterScope() {
        val data = sampleHomeData()

        // Liked filter only includes liked track (Neon Pulse), excludes Coffeehouse Tape
        val likedSearch = LibrarySearchEngine.search("Coffeehouse", LibraryFilter.LIKED, data)
        assertTrue(likedSearch.isEmpty)

        val likedMatch = LibrarySearchEngine.search("Vance", LibraryFilter.LIKED, data)
        assertEquals(listOf("Kairo Vance"), likedMatch.artists.map { it.artist })

        // Downloaded filter only includes downloaded track (Coffeehouse Tape)
        val downloadedSearch = LibrarySearchEngine.search("Rainy Turntable", LibraryFilter.DOWNLOADED, data)
        assertEquals(listOf("Rainy Turntable"), downloadedSearch.albums.map { it.title })
        assertEquals(listOf("track_coffeehouse_tape"), downloadedSearch.tracks.map { it.id })
    }

    @Test
    fun blankQueryAndNonMatchingQueryReturnEmptyResults() {
        val data = sampleHomeData()

        assertTrue(LibrarySearchEngine.search("   ", LibraryFilter.ALL, data).isEmpty)
        assertTrue(LibrarySearchEngine.search("zzznonexistenttrack", LibraryFilter.ALL, data).isEmpty)
    }
}
