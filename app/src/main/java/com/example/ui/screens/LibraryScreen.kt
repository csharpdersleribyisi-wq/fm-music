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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GroupAdd
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.MusicCatalog
import com.example.data.local.status
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.player.PlaybackState
import com.example.ui.LibraryFilter
import com.example.ui.SonoraHomeData
import com.example.ui.components.PlaylistCoverArt
import com.example.ui.components.TrackRowItem
import com.example.ui.components.formatStorageMb
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet

@Composable
fun LibraryScreen(
    currentUserId: String,
    data: SonoraHomeData,
    playbackState: PlaybackState,
    activeFilter: LibraryFilter,
    onSelectFilter: (LibraryFilter) -> Unit,
    onCreatePlaylistClick: () -> Unit,
    onCreateCollabPlaylistClick: () -> Unit,
    onJoinCollabClick: () -> Unit,
    onOpenPlaylist: (UserPlaylist) -> Unit,
    onOpenCollabPlaylist: (CollaborativePlaylist) -> Unit,
    onOpenLikedSongs: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
    onDownloadPlaylistTracks: (List<Track>, String) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    onSharePlaylist: (UserPlaylist) -> Unit,
    onShareCollabPlaylist: (CollaborativePlaylist) -> Unit,
    onTogglePublishPlaylist: (UserPlaylist) -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    val likedTracks = MusicCatalog.tracks.filter { it.id in data.likedTrackIds }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 14.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Your Library",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SonoraTextPrimary
                    )

                    Button(
                        onClick = onCreatePlaylistClick,
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraEmerald,
                            contentColor = SonoraBg
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
                        modifier = Modifier.testTag("library_create_playlist_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Create playlist",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.create_playlist), fontWeight = FontWeight.Bold)
                    }
                }

                // Collaborative Playlist Action Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onCreateCollabPlaylistClick,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_create_collab_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.GroupAdd,
                            contentDescription = "New collaborative playlist",
                            tint = SonoraViolet,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.create_collab_playlist), color = SonoraViolet)
                    }

                    OutlinedButton(
                        onClick = onJoinCollabClick,
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("library_join_collab_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Link,
                            contentDescription = "Join by invite link",
                            tint = SonoraTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.join_collab_playlist), color = SonoraTeal)
                    }
                }
            }
        }

        // Filter Chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val options = listOf(
                    LibraryFilter.ALL to "All",
                    LibraryFilter.COLLABORATIVE to "Collab (${data.collaborativePlaylists.size})",
                    LibraryFilter.PLAYLISTS to "Playlists (${data.playlists.size})",
                    LibraryFilter.DOWNLOADED to "Downloaded (${data.downloadedTracks.size})",
                    LibraryFilter.LIKED to "Liked (${likedTracks.size})"
                )
                items(options) { (filter, label) ->
                    FilterChip(
                        selected = activeFilter == filter,
                        onClick = { onSelectFilter(filter) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SonoraEmerald.copy(alpha = 0.2f),
                            selectedLabelColor = SonoraEmerald
                        ),
                        modifier = Modifier.testTag("library_filter_${filter.name.lowercase()}")
                    )
                }
            }
        }

        // Liked Songs Card
        if (activeFilter == LibraryFilter.ALL || activeFilter == LibraryFilter.LIKED) {
            item {
                Surface(
                    color = SonoraSurfaceElevated,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, SonoraEmerald.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                        .clip(RoundedCornerShape(18.dp))
                        .clickable(onClick = onOpenLikedSongs)
                        .testTag("library_liked_songs_card")
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(58.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.linearGradient(listOf(SonoraEmerald, SonoraTeal))
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Liked Songs",
                                tint = SonoraBg,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Liked Songs",
                                style = MaterialTheme.typography.titleMedium,
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = "${likedTracks.size} tracks • Synced across devices",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                        if (likedTracks.isNotEmpty()) {
                            IconButton(
                                onClick = { onPlayTrack(likedTracks.first(), likedTracks) },
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PlayArrow,
                                    contentDescription = "Play liked songs",
                                    tint = SonoraEmerald,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Collaborative Playlists Section
        if (activeFilter == LibraryFilter.ALL || activeFilter == LibraryFilter.COLLABORATIVE) {
            if (data.collaborativePlaylists.isNotEmpty() || activeFilter == LibraryFilter.COLLABORATIVE) {
                item {
                    Text(
                        text = "Collaborative Playlists (${data.collaborativePlaylists.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = SonoraViolet
                    )
                }
            }

            if (data.collaborativePlaylists.isEmpty() && activeFilter == LibraryFilter.COLLABORATIVE) {
                item {
                    Surface(
                        color = SonoraSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SonoraViolet.copy(alpha = 0.4f), RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.GroupAdd,
                                contentDescription = "No collaborative playlists",
                                tint = SonoraViolet,
                                modifier = Modifier.size(34.dp)
                            )
                            Text(
                                text = "Build a playlist with friends",
                                style = MaterialTheme.typography.titleMedium,
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = "Create a collaborative mix and invite friends by @username or shareable invite link so everyone can add, remove, and reorder songs.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(data.collaborativePlaylists, key = { "collab_${it.id}" }) { collab ->
                    val tracks = MusicCatalog.resolveTracks(collab.trackIds)
                    Surface(
                        color = SonoraSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SonoraViolet.copy(alpha = 0.45f), RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenCollabPlaylist(collab) }
                            .testTag("library_collab_${collab.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCoverArt(
                                coverKey = collab.coverKey,
                                contentDescription = collab.title,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.GroupAdd,
                                        contentDescription = "Collaborative",
                                        tint = SonoraViolet,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = collab.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SonoraTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Text(
                                    text = "${tracks.size} tracks • ${collab.collaboratorUids.size} collaborators • ${collab.inviteCode}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            IconButton(
                                onClick = { onShareCollabPlaylist(collab) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_collab_${collab.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share collaborative invite",
                                    tint = SonoraViolet
                                )
                            }

                            if (tracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play collaborative playlist",
                                        tint = SonoraEmerald,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Personal Playlists Section
        if (activeFilter == LibraryFilter.ALL || activeFilter == LibraryFilter.PLAYLISTS) {
            if (data.playlists.isNotEmpty()) {
                item {
                    Text(
                        text = "Personal Playlists (${data.playlists.size})",
                        style = MaterialTheme.typography.titleMedium,
                        color = SonoraTextPrimary
                    )
                }
            }

            if (data.playlists.isEmpty() && data.collaborativePlaylists.isEmpty()) {
                item {
                    Surface(
                        color = SonoraSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, SonoraOutline, RoundedCornerShape(18.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Empty playlists",
                                tint = SonoraTextSecondary,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "No custom playlists yet",
                                style = MaterialTheme.typography.titleMedium,
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = "Create your own playlist or save a personalized Daily Mix from For You.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                    }
                }
            } else {
                items(data.playlists, key = { it.id }) { playlist ->
                    val tracks = MusicCatalog.resolveTracks(playlist.trackIds)
                    val downloadedCount = tracks.count {
                        data.offlineMap[it.id]?.status == OfflineDownloadStatus.DOWNLOADED
                    }
                    val isPublishedOnline = data.isPlaylistPublished(playlist.id, currentUserId)
                    Surface(
                        color = SonoraSurface,
                        shape = RoundedCornerShape(18.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(
                                1.dp,
                                if (isPublishedOnline) SonoraEmerald.copy(alpha = 0.5f) else SonoraOutline,
                                RoundedCornerShape(18.dp)
                            )
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onOpenPlaylist(playlist) }
                            .testTag("library_playlist_${playlist.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCoverArt(
                                coverKey = playlist.coverKey,
                                contentDescription = playlist.title,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = playlist.title,
                                        style = MaterialTheme.typography.titleMedium,
                                        color = SonoraTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (playlist.personalized) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Rounded.AutoAwesome,
                                            contentDescription = "Personalized mix",
                                            tint = SonoraEmerald,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    if (isPublishedOnline) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = SonoraEmerald.copy(alpha = 0.16f),
                                            shape = RoundedCornerShape(50)
                                        ) {
                                            Text(
                                                text = "ONLINE",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = SonoraEmerald,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                                Text(
                                    text = "${tracks.size} tracks • ${playlist.moodTag}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                                if (tracks.isNotEmpty()) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Rounded.OfflinePin,
                                            contentDescription = "Offline status",
                                            tint = if (downloadedCount == tracks.size) SonoraTeal else SonoraTextSecondary,
                                            modifier = Modifier.size(13.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "$downloadedCount/${tracks.size} offline",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = if (downloadedCount == tracks.size) SonoraTeal else SonoraTextSecondary
                                        )
                                    }
                                }
                            }

                            IconButton(
                                onClick = { onTogglePublishPlaylist(playlist) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("publish_online_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Public,
                                    contentDescription = if (isPublishedOnline) {
                                        "Remove playlist from online public feed"
                                    } else {
                                        "Publish playlist online for everyone"
                                    },
                                    tint = if (isPublishedOnline) SonoraEmerald else SonoraTextSecondary
                                )
                            }

                            IconButton(
                                onClick = { onSharePlaylist(playlist) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_playlist_${playlist.id}")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = "Share playlist",
                                    tint = if (isPublishedOnline) SonoraEmerald else SonoraTextSecondary
                                )
                            }

                            if (tracks.isNotEmpty() && downloadedCount < tracks.size) {
                                IconButton(
                                    onClick = { onDownloadPlaylistTracks(tracks, playlist.title) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DownloadForOffline,
                                        contentDescription = "Download playlist offline",
                                        tint = SonoraTeal
                                    )
                                }
                            }

                            if (tracks.isNotEmpty()) {
                                IconButton(
                                    onClick = { onPlayTrack(tracks.first(), tracks) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play playlist",
                                        tint = SonoraEmerald,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Downloaded tracks inline list if DOWNLOADED filter selected
        if (activeFilter == LibraryFilter.DOWNLOADED) {
            item {
                Text(
                    text = "Offline Vault (${formatStorageMb(data.totalOfflineBytes)})",
                    style = MaterialTheme.typography.titleMedium,
                    color = SonoraTeal
                )
            }
            if (data.downloadedTracks.isEmpty()) {
                item {
                    Text(
                        text = "No offline tracks cached yet. Tap the download icon on any track to listen without internet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = SonoraTextSecondary
                    )
                }
            } else {
                items(data.downloadedTracks, key = { it.id }) { track ->
                    TrackRowItem(
                        track = track,
                        isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                        isLiked = track.id in data.likedTrackIds,
                        offlineEntity = data.offlineMap[track.id],
                        offlineModeEnabled = data.offlineModeEnabled,
                        onPlayClick = { onPlayTrack(track, data.downloadedTracks) },
                        onToggleLike = { onToggleLike(track) },
                        onDownloadClick = { onDownloadTrack(track) },
                        onRemoveDownloadClick = { onRemoveDownload(track) },
                        onAddToPlaylistClick = { onAddToPlaylist(track) },
                        onShareClick = { onShareTrack(track) }
                    )
                }
            }
        }

        // Liked tracks inline list if LIKED filter selected
        if (activeFilter == LibraryFilter.LIKED && likedTracks.isNotEmpty()) {
            items(likedTracks, key = { it.id }) { track ->
                TrackRowItem(
                    track = track,
                    isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                    isLiked = true,
                    offlineEntity = data.offlineMap[track.id],
                    offlineModeEnabled = data.offlineModeEnabled,
                    onPlayClick = { onPlayTrack(track, likedTracks) },
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
