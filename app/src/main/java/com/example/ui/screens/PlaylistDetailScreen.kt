package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ExitToApp
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.PublicOff
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.catalog.MusicCatalog
import com.example.data.model.AlbumInfo
import com.example.data.model.CollaborativePlaylist
import com.example.data.model.PersonalizedMix
import com.example.data.model.PublicPlaylist
import com.example.data.model.Track
import com.example.data.model.UserPlaylist
import com.example.player.PlaybackState
import com.example.ui.SonoraHomeData
import com.example.ui.components.PlaylistCoverArt
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet

sealed interface CollectionDetailTarget {
    data class PlaylistTarget(val playlistId: String) : CollectionDetailTarget
    data class CollabPlaylistTarget(val playlistId: String) : CollectionDetailTarget
    data class PublicPlaylistTarget(val publicPlaylistId: String) : CollectionDetailTarget
    data class MixTarget(val mixId: String) : CollectionDetailTarget
    data class AlbumTarget(val albumTitle: String) : CollectionDetailTarget
    data object LikedSongsTarget : CollectionDetailTarget
}

@Composable
fun PlaylistDetailScreen(
    target: CollectionDetailTarget,
    currentUserId: String,
    data: SonoraHomeData,
    playbackState: PlaybackState,
    onBack: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
    onDownloadAll: (List<Track>, String) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    onSharePlaylist: (UserPlaylist) -> Unit,
    onShareCollabPlaylist: (CollaborativePlaylist) -> Unit,
    onSharePublicPlaylist: (PublicPlaylist) -> Unit,
    onShareAlbum: (AlbumInfo) -> Unit,
    onSaveMixAsPlaylist: (PersonalizedMix) -> Unit,
    onSavePublicPlaylist: (PublicPlaylist) -> Unit,
    onTogglePublishPlaylist: (UserPlaylist) -> Unit,
    onUnpublishPublicPlaylist: (PublicPlaylist) -> Unit,
    onEditPlaylist: (UserPlaylist) -> Unit,
    onDeletePlaylist: (UserPlaylist) -> Unit,
    onRemoveTrackFromPlaylist: (UserPlaylist, String) -> Unit,
    onOpenAddTrackToCollab: (CollaborativePlaylist) -> Unit,
    onOpenInviteFriendToCollab: (CollaborativePlaylist) -> Unit,
    onMoveCollabTrack: (CollaborativePlaylist, String, Int) -> Unit,
    onRemoveCollabTrack: (CollaborativePlaylist, String) -> Unit,
    onDeleteCollabPlaylist: (CollaborativePlaylist) -> Unit,
    onLeaveCollabPlaylist: (CollaborativePlaylist) -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    BackHandler(onBack = onBack)

    val userPlaylist = (target as? CollectionDetailTarget.PlaylistTarget)?.let { t ->
        data.playlists.firstOrNull { it.id == t.playlistId }
    }
    val collabPlaylist = (target as? CollectionDetailTarget.CollabPlaylistTarget)?.let { t ->
        data.collaborativePlaylists.firstOrNull { it.id == t.playlistId }
    }
    val publicPlaylist = (target as? CollectionDetailTarget.PublicPlaylistTarget)?.let { t ->
        data.publicPlaylists.firstOrNull { it.id == t.publicPlaylistId }
    }
    val mix = (target as? CollectionDetailTarget.MixTarget)?.let { t ->
        data.personalizedMixes.firstOrNull { it.id == t.mixId }
    }
    val album = (target as? CollectionDetailTarget.AlbumTarget)?.let { t ->
        MusicCatalog.getAlbums().firstOrNull { it.title == t.albumTitle }
    }
    val isUserPlaylistPublished = userPlaylist != null &&
        data.isPlaylistPublished(userPlaylist.id, currentUserId)

    val title: String
    val subtitle: String
    val badge: String
    val coverKey: String?
    val tracks: List<Track>

    when (target) {
        is CollectionDetailTarget.PlaylistTarget -> {
            title = userPlaylist?.title ?: "Playlist"
            subtitle = userPlaylist?.description?.ifBlank {
                if (isUserPlaylistPublished) "Published online for everyone" else "Custom synced playlist"
            } ?: ""
            badge = if (isUserPlaylistPublished) {
                "ONLINE PUBLIC • ${userPlaylist?.moodTag ?: "Playlist"}"
            } else {
                userPlaylist?.moodTag ?: "Playlist"
            }
            coverKey = userPlaylist?.coverKey ?: "midnight_synth"
            tracks = userPlaylist?.let { MusicCatalog.resolveTracks(it.trackIds) } ?: emptyList()
        }

        is CollectionDetailTarget.CollabPlaylistTarget -> {
            title = collabPlaylist?.title ?: "Collaborative Mix"
            val members = collabPlaylist?.collaboratorUids?.size ?: 1
            val owner = collabPlaylist?.ownerName ?: "Host"
            subtitle = buildString {
                append(collabPlaylist?.description?.ifBlank { "Live shared queue" } ?: "")
                append(" • Host: $owner ($members collaborators)")
            }
            badge = collabPlaylist?.let { "COLLAB • CODE ${it.inviteCode}" } ?: "COLLAB"
            coverKey = collabPlaylist?.coverKey ?: "sonic_horizon"
            tracks = collabPlaylist?.let { MusicCatalog.resolveTracks(it.trackIds) } ?: emptyList()
        }

        is CollectionDetailTarget.PublicPlaylistTarget -> {
            title = publicPlaylist?.title ?: "Public Playlist"
            val creator = publicPlaylist?.let {
                val handle = if (it.ownerUsername.isNotBlank()) " (@${it.ownerUsername})" else ""
                "by ${it.ownerName}$handle"
            } ?: ""
            val desc = publicPlaylist?.description?.takeIf { it.isNotBlank() }
            subtitle = listOfNotNull(desc, creator.ifBlank { null }).joinToString(" • ")
            badge = "PUBLIC ONLINE • ${publicPlaylist?.moodTag ?: "Shared"}"
            coverKey = publicPlaylist?.coverKey ?: "midnight_synth"
            tracks = publicPlaylist?.let { MusicCatalog.resolveTracks(it.trackIds) } ?: emptyList()
        }

        is CollectionDetailTarget.MixTarget -> {
            title = mix?.title ?: "Daily Mix"
            subtitle = mix?.let { "${it.subtitle} • ${it.reason}" } ?: ""
            badge = mix?.let { "${it.matchPercent}% Match • ${it.moodTag}" } ?: "Personalized"
            coverKey = mix?.coverKey ?: "sonic_horizon"
            tracks = mix?.tracks ?: emptyList()
        }

        is CollectionDetailTarget.AlbumTarget -> {
            title = album?.title ?: "Album"
            subtitle = album?.let { "${it.artist} • ${it.genre}" } ?: ""
            badge = "Album • ${album?.tracks?.size ?: 0} Tracks"
            coverKey = album?.coverKey ?: "midnight_synth"
            tracks = album?.tracks ?: emptyList()
        }

        CollectionDetailTarget.LikedSongsTarget -> {
            title = "Liked Songs"
            subtitle = "All your favorited tracks, synced across devices"
            badge = "Personal Favorites"
            coverKey = null
            tracks = MusicCatalog.tracks.filter { it.id in data.likedTrackIds }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 10.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = SonoraTextPrimary
                    )
                }

                Row {
                    if (userPlaylist != null) {
                        IconButton(
                            onClick = { onTogglePublishPlaylist(userPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_toggle_publish_btn")
                        ) {
                            Icon(
                                imageVector = if (isUserPlaylistPublished) Icons.Rounded.Public else Icons.Rounded.PublicOff,
                                contentDescription = if (isUserPlaylistPublished) "Published online" else "Publish online",
                                tint = if (isUserPlaylistPublished) SonoraEmerald else SonoraTextSecondary
                            )
                        }
                        IconButton(
                            onClick = { onSharePlaylist(userPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_share_playlist_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share playlist",
                                tint = if (isUserPlaylistPublished) SonoraEmerald else SonoraTextPrimary
                            )
                        }
                        IconButton(
                            onClick = { onEditPlaylist(userPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_edit_playlist_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Edit,
                                contentDescription = "Edit playlist",
                                tint = SonoraTextPrimary
                            )
                        }
                        IconButton(
                            onClick = {
                                onDeletePlaylist(userPlaylist)
                                onBack()
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_delete_playlist_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = "Delete playlist",
                                tint = SonoraCoral
                            )
                        }
                    }

                    if (publicPlaylist != null) {
                        IconButton(
                            onClick = { onSharePublicPlaylist(publicPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_public_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share online playlist with everyone",
                                tint = SonoraEmerald
                            )
                        }
                        IconButton(
                            onClick = { onSavePublicPlaylist(publicPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_public_save_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.BookmarkAdd,
                                contentDescription = "Save copy to my library",
                                tint = SonoraTeal
                            )
                        }
                        if (publicPlaylist.ownerId == currentUserId) {
                            IconButton(
                                onClick = {
                                    onUnpublishPublicPlaylist(publicPlaylist)
                                    onBack()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_public_unpublish_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.PublicOff,
                                    contentDescription = "Remove from online public feed",
                                    tint = SonoraCoral
                                )
                            }
                        }
                    }

                    if (collabPlaylist != null) {
                        IconButton(
                            onClick = { onOpenInviteFriendToCollab(collabPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_collab_invite_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PersonAdd,
                                contentDescription = "Invite friends",
                                tint = SonoraViolet
                            )
                        }
                        IconButton(
                            onClick = { onShareCollabPlaylist(collabPlaylist) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_collab_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share collaborative link",
                                tint = SonoraEmerald
                            )
                        }
                        if (collabPlaylist.ownerId == currentUserId) {
                            IconButton(
                                onClick = {
                                    onDeleteCollabPlaylist(collabPlaylist)
                                    onBack()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_collab_delete_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Delete,
                                    contentDescription = "Delete collaborative playlist",
                                    tint = SonoraCoral
                                )
                            }
                        } else {
                            IconButton(
                                onClick = {
                                    onLeaveCollabPlaylist(collabPlaylist)
                                    onBack()
                                },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_collab_leave_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ExitToApp,
                                    contentDescription = "Leave collaborative playlist",
                                    tint = SonoraCoral
                                )
                            }
                        }
                    }

                    if (album != null) {
                        IconButton(
                            onClick = { onShareAlbum(album) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("detail_share_album_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share album",
                                tint = SonoraEmerald
                            )
                        }
                    }
                }
            }
        }

        // Cover & Title Header
        item {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                if (coverKey != null) {
                    PlaylistCoverArt(
                        coverKey = coverKey,
                        contentDescription = title,
                        modifier = Modifier
                            .size(188.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .border(
                                1.dp,
                                if (collabPlaylist != null) SonoraViolet else SonoraOutline,
                                RoundedCornerShape(24.dp)
                            )
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(188.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Brush.linearGradient(listOf(SonoraEmerald, SonoraTeal))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Liked Songs",
                            tint = SonoraBg,
                            modifier = Modifier.size(64.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    color = if (collabPlaylist != null) {
                        SonoraViolet.copy(alpha = 0.18f)
                    } else {
                        SonoraEmerald.copy(alpha = 0.16f)
                    },
                    shape = RoundedCornerShape(50)
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (collabPlaylist != null) SonoraViolet else SonoraEmerald,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = SonoraTextPrimary
                )
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = SonoraTextSecondary
                    )
                }
                if (collabPlaylist != null && collabPlaylist.invitedHandles.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Invited: ${collabPlaylist.invitedHandles.joinToString(", ") { "@$it" }}",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTeal
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(
                        onClick = {
                            tracks.firstOrNull()?.let { first -> onPlayTrack(first, tracks) }
                        },
                        enabled = tracks.isNotEmpty(),
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SonoraEmerald,
                            contentColor = SonoraBg
                        ),
                        modifier = Modifier.testTag("detail_play_all_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play all"
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All (${tracks.size})", fontWeight = FontWeight.Bold)
                    }

                    if (collabPlaylist != null) {
                        Surface(
                            color = SonoraViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, SonoraViolet, RoundedCornerShape(50))
                        ) {
                            IconButton(
                                onClick = { onOpenAddTrackToCollab(collabPlaylist) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("collab_add_song_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Add,
                                    contentDescription = "Add song to collaborative playlist",
                                    tint = SonoraViolet
                                )
                            }
                        }
                    }

                    if (tracks.isNotEmpty()) {
                        Surface(
                            color = SonoraSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, SonoraOutline, RoundedCornerShape(50))
                        ) {
                            IconButton(
                                onClick = { onDownloadAll(tracks, title) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_download_all_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.DownloadForOffline,
                                    contentDescription = "Download all tracks offline",
                                    tint = SonoraTeal
                                )
                            }
                        }
                    }

                    if (mix != null) {
                        Surface(
                            color = SonoraSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(1.dp, SonoraOutline, RoundedCornerShape(50))
                        ) {
                            IconButton(
                                onClick = { onSaveMixAsPlaylist(mix) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("detail_save_mix_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.BookmarkAdd,
                                    contentDescription = "Save mix to library",
                                    tint = SonoraEmerald
                                )
                            }
                        }
                    }
                }

                if (userPlaylist != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = { onTogglePublishPlaylist(userPlaylist) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier.testTag("detail_publish_online_pill")
                    ) {
                        Icon(
                            imageVector = if (isUserPlaylistPublished) Icons.Rounded.Public else Icons.Rounded.PublicOff,
                            contentDescription = "Toggle online visibility",
                            tint = if (isUserPlaylistPublished) SonoraEmerald else SonoraTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isUserPlaylistPublished) {
                                "Published Online • Tap to Make Private"
                            } else {
                                "Publish Online & Share with Everyone"
                            },
                            color = if (isUserPlaylistPublished) SonoraEmerald else SonoraTeal,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        if (tracks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (collabPlaylist != null) {
                            "No songs yet. Tap + above to add the first track or invite friends."
                        } else {
                            "No tracks here yet. Add tracks from Discover or For You."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = SonoraTextSecondary
                    )
                }
            }
        } else {
            itemsIndexed(tracks, key = { index, t -> "${index}_${t.id}" }) { index, track ->
                TrackRowItem(
                    track = track,
                    isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                    isLiked = track.id in data.likedTrackIds,
                    offlineEntity = data.offlineMap[track.id],
                    offlineModeEnabled = data.offlineModeEnabled,
                    onPlayClick = { onPlayTrack(track, tracks) },
                    onToggleLike = { onToggleLike(track) },
                    onDownloadClick = { onDownloadTrack(track) },
                    onRemoveDownloadClick = { onRemoveDownload(track) },
                    onAddToPlaylistClick = { onAddToPlaylist(track) },
                    onShareClick = { onShareTrack(track) },
                    onMoveUp = if (collabPlaylist != null && index > 0) {
                        { onMoveCollabTrack(collabPlaylist, track.id, -1) }
                    } else null,
                    onMoveDown = if (collabPlaylist != null && index < tracks.lastIndex) {
                        { onMoveCollabTrack(collabPlaylist, track.id, 1) }
                    } else null,
                    trailingRemoveAction = when {
                        userPlaylist != null -> ({ onRemoveTrackFromPlaylist(userPlaylist, track.id) })
                        collabPlaylist != null -> ({ onRemoveCollabTrack(collabPlaylist, track.id) })
                        else -> null
                    }
                )
            }
        }
    }
}
