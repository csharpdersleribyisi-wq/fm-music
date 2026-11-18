package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.catalog.MusicCatalog
import com.example.data.model.AlbumInfo
import com.example.data.model.PublicPlaylist
import com.example.data.model.Track
import com.example.player.PlaybackState
import com.example.ui.SonoraHomeData
import com.example.ui.components.PlaylistCoverArt
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.SonoraAmber
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet

@Composable
fun DiscoverScreen(
    data: SonoraHomeData,
    playbackState: PlaybackState,
    searchQuery: String,
    selectedGenre: String?,
    onSearchQueryChange: (String) -> Unit,
    onSelectGenre: (String) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    onOpenAlbum: (AlbumInfo) -> Unit,
    onShareAlbum: (AlbumInfo) -> Unit,
    onOpenPublicPlaylist: (PublicPlaylist) -> Unit,
    onSharePublicPlaylist: (PublicPlaylist) -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    val albums = remember { MusicCatalog.getAlbums() }
    val filteredTracks = remember(searchQuery, selectedGenre) {
        val q = searchQuery.trim().lowercase()
        MusicCatalog.tracks.filter { track ->
            val matchesGenre = selectedGenre == null || track.genre == selectedGenre
            val matchesQuery = q.isEmpty() ||
                track.title.lowercase().contains(q) ||
                track.artist.lowercase().contains(q) ||
                track.genre.lowercase().contains(q) ||
                track.mood.lowercase().contains(q) ||
                track.album.lowercase().contains(q)
            matchesGenre && matchesQuery
        }
    }

    val genreColors = remember {
        mapOf(
            "Synthwave" to SonoraEmerald,
            "Lo-Fi Chill" to SonoraAmber,
            "Acoustic Dawn" to SonoraTeal,
            "Deep Focus" to Color(0xFF4C8DFF),
            "Late Night Jazz" to SonoraViolet,
            "Indie Pulse" to SonoraCoral
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Discover",
                style = MaterialTheme.typography.headlineMedium,
                color = SonoraTextPrimary
            )
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                placeholder = { Text("Tracks, artists, albums, genres, or moods") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = "Search",
                        tint = SonoraTextSecondary
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.minimumInteractiveComponentSize()
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = "Clear search",
                                tint = SonoraTextSecondary
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = SonoraSurfaceElevated,
                    unfocusedContainerColor = SonoraSurfaceElevated,
                    focusedBorderColor = SonoraEmerald,
                    unfocusedBorderColor = SonoraOutline,
                    focusedTextColor = SonoraTextPrimary,
                    unfocusedTextColor = SonoraTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("discover_search_input")
            )
        }

        // Online Community Playlists Section (Shared with Everyone)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = "Online community playlists",
                        tint = SonoraEmerald,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Online Community Playlists (${data.publicPlaylists.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = SonoraTextPrimary
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            if (data.publicPlaylists.isEmpty()) {
                Surface(
                    color = SonoraSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SonoraOutline, RoundedCornerShape(16.dp))
                ) {
                    Text(
                        text = "No playlists published online yet. Open Your Library and tap the globe icon on any personal playlist to share it with everyone!",
                        style = MaterialTheme.typography.bodySmall,
                        color = SonoraTextSecondary,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(data.publicPlaylists, key = { it.id }) { pub ->
                        val pubTracks = MusicCatalog.resolveTracks(pub.trackIds)
                        Surface(
                            color = SonoraSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(204.dp)
                                .border(1.dp, SonoraEmerald.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenPublicPlaylist(pub) }
                                .testTag("public_playlist_card_${pub.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(116.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                ) {
                                    PlaylistCoverArt(
                                        coverKey = pub.coverKey,
                                        contentDescription = pub.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Row(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                                    ) {
                                        if (pubTracks.isNotEmpty()) {
                                            IconButton(
                                                onClick = { onPlayTrack(pubTracks.first(), pubTracks) },
                                                modifier = Modifier
                                                    .minimumInteractiveComponentSize()
                                                    .testTag("play_public_${pub.id}")
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Rounded.PlayArrow,
                                                    contentDescription = "Play ${pub.title}",
                                                    tint = SonoraEmerald
                                                )
                                            }
                                        }
                                        IconButton(
                                            onClick = { onSharePublicPlaylist(pub) },
                                            modifier = Modifier
                                                .minimumInteractiveComponentSize()
                                                .testTag("share_public_${pub.id}")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Share,
                                                contentDescription = "Share ${pub.title}",
                                                tint = SonoraTextPrimary
                                            )
                                        }
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = pub.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SonoraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "by ${pub.ownerName}${if (pub.ownerUsername.isNotBlank()) " (@${pub.ownerUsername})" else ""}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraEmerald,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${pubTracks.size} tracks • ${pub.moodTag}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonoraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }
        }

        // Featured Albums Carousel with Social Sharing
        item {
            Text(
                text = "Featured Albums",
                style = MaterialTheme.typography.titleMedium,
                color = SonoraTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(albums, key = { it.title }) { album ->
                    Surface(
                        color = SonoraSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .width(184.dp)
                            .border(1.dp, SonoraOutline, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenAlbum(album) }
                            .testTag("album_card_${album.title}")
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(126.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            ) {
                                PlaylistCoverArt(
                                    coverKey = album.coverKey,
                                    contentDescription = album.title,
                                    modifier = Modifier.fillMaxSize()
                                )
                                IconButton(
                                    onClick = { onShareAlbum(album) },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .minimumInteractiveComponentSize()
                                        .testTag("share_album_${album.title}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Share,
                                        contentDescription = "Share ${album.title}",
                                        tint = SonoraTextPrimary
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = album.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = SonoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${album.artist} • ${album.tracks.size} tracks",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Browse by Genre",
                style = MaterialTheme.typography.titleMedium,
                color = SonoraTextPrimary
            )
            Spacer(modifier = Modifier.height(10.dp))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(MusicCatalog.allGenres) { genre ->
                    val accent = genreColors[genre] ?: SonoraEmerald
                    val selected = genre == selectedGenre
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        accent.copy(alpha = if (selected) 0.45f else 0.2f),
                                        SonoraSurfaceElevated
                                    )
                                )
                            )
                            .border(
                                width = if (selected) 1.5.dp else 1.dp,
                                color = if (selected) accent else SonoraOutline,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { onSelectGenre(genre) }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("genre_tile_$genre")
                    ) {
                        Column {
                            Text(
                                text = genre,
                                style = MaterialTheme.typography.titleSmall,
                                color = SonoraTextPrimary,
                                fontWeight = FontWeight.Bold
                            )
                            val count = MusicCatalog.tracks.count { it.genre == genre }
                            Text(
                                text = "$count tracks",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent
                            )
                        }
                    }
                }
            }
        }

        if (selectedGenre != null) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Showing $selectedGenre (${filteredTracks.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = SonoraTextPrimary
                    )
                    FilterChip(
                        selected = true,
                        onClick = { onSelectGenre(selectedGenre) },
                        label = { Text("Clear filter") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonoraEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = SonoraEmerald
                        )
                    )
                }
            }
        } else {
            item {
                Text(
                    text = "Catalog (${filteredTracks.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = SonoraTextPrimary
                )
            }
        }

        if (filteredTracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No tracks match \"$searchQuery\". Try another mood or genre.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = SonoraTextSecondary
                    )
                }
            }
        } else {
            items(filteredTracks, key = { it.id }) { track ->
                TrackRowItem(
                    track = track,
                    isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                    isLiked = track.id in data.likedTrackIds,
                    offlineEntity = data.offlineMap[track.id],
                    offlineModeEnabled = data.offlineModeEnabled,
                    onPlayClick = { onPlayTrack(track, filteredTracks) },
                    onToggleLike = { onToggleLike(track) },
                    onDownloadClick = { onDownloadTrack(track) },
                    onRemoveDownloadClick = { onRemoveDownload(track) },
                    onAddToPlaylistClick = { onAddToPlaylist(track) },
                    onShareClick = { onShareTrack(track) }
                )
            }
        }
    }
}
