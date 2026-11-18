package com.example.ui.screens

import com.example.data.catalog.MusicCatalog
import com.example.data.model.AlbumInfo
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.ui.LibraryFilter
import com.example.ui.SonoraHomeData

data class LibraryArtistGroup(
    val artist: String,
    val primaryCoverKey: String,
    val accentHex: Long,
    val genre: String,
    val tracks: List<Track>
)

data class LibrarySearchResults(
    val query: String,
    val tracks: List<Track>,
    val artists: List<LibraryArtistGroup>,
    val albums: List<AlbumInfo>,
    val playlists: List<UserPlaylist>,
    val collaborativePlaylists: List<CollaborativePlaylist>
) {
    val isEmpty: Boolean
        get() = tracks.isEmpty() &&
            artists.isEmpty() &&
            albums.isEmpty() &&
            playlists.isEmpty() &&
            collaborativePlaylists.isEmpty()
}

object LibrarySearchEngine {

    fun search(
        query: String,
        activeFilter: LibraryFilter,
        data: SonoraHomeData,
        catalogTracks: List<Track> = MusicCatalog.tracks
    ): LibrarySearchResults {
        val trimmed = query.trim()
        val q = trimmed.lowercase()
        if (q.isEmpty()) {
            return LibrarySearchResults(
                query = "",
                tracks = emptyList(),
                artists = emptyList(),
                albums = emptyList(),
                playlists = emptyList(),
                collaborativePlaylists = emptyList()
            )
        }

        val trackLookup = catalogTracks.associateBy { it.id }

        val candidateTracks: List<Track> = when (activeFilter) {
            LibraryFilter.LIKED -> catalogTracks.filter { it.id in data.likedTrackIds }
            LibraryFilter.DOWNLOADED -> data.downloadedTracks
            LibraryFilter.PLAYLISTS -> data.playlists
                .flatMap { pl -> pl.trackIds.mapNotNull { trackLookup[it] } }
                .distinctBy { it.id }
            LibraryFilter.COLLABORATIVE -> data.collaborativePlaylists
                .flatMap { collab -> collab.trackIds.mapNotNull { trackLookup[it] } }
                .distinctBy { it.id }
            LibraryFilter.ALL -> {
                val libraryTrackIds = buildSet {
                    addAll(data.likedTrackIds)
                    addAll(data.downloadedTracks.map { it.id })
                    data.playlists.forEach { addAll(it.trackIds) }
                    data.collaborativePlaylists.forEach { addAll(it.trackIds) }
                }
                val inLibrary = catalogTracks.filter { it.id in libraryTrackIds }
                val remaining = catalogTracks.filter { it.id !in libraryTrackIds }
                inLibrary + remaining
            }
        }

        val matchingTracks = candidateTracks.filter { track ->
            track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.album.lowercase().contains(q)
        }

        val matchingArtists = candidateTracks
            .groupBy { it.artist }
            .filterKeys { artistName -> artistName.lowercase().contains(q) }
            .map { (artistName, artistTracks) ->
                val lead = artistTracks.first()
                LibraryArtistGroup(
                    artist = artistName,
                    primaryCoverKey = lead.coverKey,
                    accentHex = lead.accentHex,
                    genre = lead.genre,
                    tracks = artistTracks
                )
            }
            .sortedBy { it.artist }

        val matchingAlbums = candidateTracks
            .groupBy { it.album }
            .map { (albumTitle, albumTracks) ->
                val lead = albumTracks.first()
                AlbumInfo(
                    title = albumTitle,
                    artist = albumTracks.map { it.artist }.distinct().joinToString(" • "),
                    genre = lead.genre,
                    coverKey = lead.coverKey,
                    accentHex = lead.accentHex,
                    tracks = albumTracks
                )
            }
            .filter { album ->
                album.title.lowercase().contains(q) || album.artist.lowercase().contains(q)
            }
            .sortedBy { it.title }

        val matchingPlaylists = if (activeFilter == LibraryFilter.ALL || activeFilter == LibraryFilter.PLAYLISTS) {
            data.playlists.filter { playlist ->
                playlist.title.lowercase().contains(q) ||
                    playlist.moodTag.lowercase().contains(q) ||
                    playlist.trackIds.any { id ->
                        val t = trackLookup[id]
                        t != null && (
                            t.title.lowercase().contains(q) ||
                                t.artist.lowercase().contains(q) ||
                                t.album.lowercase().contains(q)
                            )
                    }
            }
        } else {
            emptyList()
        }

        val matchingCollabPlaylists = if (activeFilter == LibraryFilter.ALL || activeFilter == LibraryFilter.COLLABORATIVE) {
            data.collaborativePlaylists.filter { collab ->
                collab.title.lowercase().contains(q) ||
                    collab.ownerName.lowercase().contains(q) ||
                    collab.trackIds.any { id ->
                        val t = trackLookup[id]
                        t != null && (
                            t.title.lowercase().contains(q) ||
                                t.artist.lowercase().contains(q) ||
                                t.album.lowercase().contains(q)
                            )
                    }
            }
        } else {
            emptyList()
        }

        return LibrarySearchResults(
            query = trimmed,
            tracks = matchingTracks,
            artists = matchingArtists,
            albums = matchingAlbums,
            playlists = matchingPlaylists,
            collaborativePlaylists = matchingCollabPlaylists
        )
    }
}
