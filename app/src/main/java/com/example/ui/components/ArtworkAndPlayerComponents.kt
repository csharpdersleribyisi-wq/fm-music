package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.OfflinePin
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.example.R
import com.example.data.local.OfflineTrackEntity
import com.example.data.local.status
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.Track
import com.example.player.EqualizerSettings
import com.example.player.PlaybackState
import com.example.player.SleepTimerProtocol
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHigh
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet
import kotlin.math.sin

fun coverDrawableForKey(coverKey: String): Int {
    return when (coverKey) {
        "midnight_synth" -> R.drawable.img_cover_midnight_synth
        "velvet_lofi" -> R.drawable.img_cover_velvet_lofi
        "acoustic_dawn" -> R.drawable.img_cover_acoustic_dawn
        else -> R.drawable.img_hero_sonic_horizon
    }
}

fun formatDurationMs(ms: Long): String {
    val totalSeconds = (ms / 1000L).coerceAtLeast(0L)
    val mins = totalSeconds / 60L
    val secs = totalSeconds % 60L
    return "%d:%02d".format(mins, secs)
}

fun formatStorageMb(bytes: Long): String {
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb < 0.1 && bytes > 0L) "%.2f MB".format(mb) else "%.1f MB".format(mb)
}

@Composable
fun PlaylistCoverArt(
    coverKey: String,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Image(
        painter = painterResource(id = coverDrawableForKey(coverKey)),
        contentDescription = contentDescription,
        contentScale = ContentScale.Crop,
        modifier = modifier
    )
}

@Composable
fun TrackRowItem(
    track: Track,
    isPlayingCurrent: Boolean,
    isLiked: Boolean,
    offlineEntity: OfflineTrackEntity?,
    offlineModeEnabled: Boolean,
    onPlayClick: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    modifier: Modifier = Modifier,
    onShareClick: (() -> Unit)? = null,
    onMoveUp: (() -> Unit)? = null,
    onMoveDown: (() -> Unit)? = null,
    trailingRemoveAction: (() -> Unit)? = null
) {
    val downloadStatus = offlineEntity?.status ?: OfflineDownloadStatus.NONE
    val isAvailableOffline = downloadStatus == OfflineDownloadStatus.DOWNLOADED
    val dimmed = offlineModeEnabled && !isAvailableOffline

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(
                if (isPlayingCurrent) SonoraEmerald.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .clickable(onClick = onPlayClick)
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .testTag("track_row_${track.id}"),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(50.dp)) {
            PlaylistCoverArt(
                coverKey = track.coverKey,
                contentDescription = "${track.title} album art",
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(10.dp))
            )
            if (isPlayingCurrent) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(10.dp))
                        .background(SonoraBg.copy(alpha = 0.55f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Now playing",
                        tint = SonoraEmerald,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.titleMedium,
                color = when {
                    isPlayingCurrent -> SonoraEmerald
                    dimmed -> SonoraTextMuted
                    else -> SonoraTextPrimary
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isAvailableOffline) {
                    Icon(
                        imageVector = Icons.Rounded.OfflinePin,
                        contentDescription = "Downloaded for offline playback",
                        tint = SonoraTeal,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = "${track.artist} • ${track.genre}",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (dimmed) SonoraTextMuted else SonoraTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = formatDurationMs(track.durationMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = SonoraTextMuted
                )
            }
        }

        if (onMoveUp != null) {
            IconButton(
                onClick = onMoveUp,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("move_up_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowUp,
                    contentDescription = "Move track up",
                    tint = SonoraTextSecondary
                )
            }
        }

        if (onMoveDown != null) {
            IconButton(
                onClick = onMoveDown,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("move_down_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.KeyboardArrowDown,
                    contentDescription = "Move track down",
                    tint = SonoraTextSecondary
                )
            }
        }

        if (onMoveUp == null && onMoveDown == null) {
            when (downloadStatus) {
                OfflineDownloadStatus.DOWNLOADING -> {
                    Box(
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .size(48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            progress = { (offlineEntity?.progressPercent ?: 0) / 100f },
                            modifier = Modifier.size(22.dp),
                            color = SonoraTeal,
                            strokeWidth = 2.5.dp
                        )
                    }
                }

                OfflineDownloadStatus.DOWNLOADED -> {
                    IconButton(
                        onClick = onRemoveDownloadClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("remove_offline_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.CheckCircle,
                            contentDescription = "Remove offline download",
                            tint = SonoraTeal
                        )
                    }
                }

                else -> {
                    IconButton(
                        onClick = onDownloadClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("download_track_${track.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.DownloadForOffline,
                            contentDescription = "Download for offline listening",
                            tint = SonoraTextSecondary
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleLike,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("like_track_${track.id}")
            ) {
                Icon(
                    imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                    contentDescription = if (isLiked) "Unlike track" else "Like track",
                    tint = if (isLiked) SonoraEmerald else SonoraTextSecondary
                )
            }
        }

        if (onShareClick != null) {
            IconButton(
                onClick = onShareClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("share_track_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Share,
                    contentDescription = "Share track",
                    tint = SonoraTextSecondary
                )
            }
        }

        if (trailingRemoveAction != null) {
            IconButton(
                onClick = trailingRemoveAction,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("remove_from_playlist_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteOutline,
                    contentDescription = "Remove from playlist",
                    tint = SonoraTextSecondary
                )
            }
        } else {
            IconButton(
                onClick = onAddToPlaylistClick,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_to_playlist_${track.id}")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    contentDescription = "Add to playlist",
                    tint = SonoraTextSecondary
                )
            }
        }
    }
}

@Composable
fun MiniPlayerBar(
    playbackState: PlaybackState,
    isLiked: Boolean,
    onExpandClick: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onToggleLike: () -> Unit,
    modifier: Modifier = Modifier,
    equalizerSettings: EqualizerSettings = EqualizerSettings()
) {
    val track = playbackState.currentTrack ?: return
    val progress = if (playbackState.durationMs > 0L) {
        (playbackState.positionMs.toFloat() / playbackState.durationMs.toFloat()).coerceIn(0f, 1f)
    } else 0f

    Surface(
        color = SonoraSurfaceHigh,
        tonalElevation = 6.dp,
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .border(1.dp, SonoraOutline, RoundedCornerShape(18.dp))
            .clickable(onClick = onExpandClick)
            .testTag("mini_player_bar")
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 10.dp, end = 6.dp, top = 8.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PlaylistCoverArt(
                    coverKey = track.coverKey,
                    contentDescription = "${track.title} cover",
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.title,
                        style = MaterialTheme.typography.titleSmall,
                        color = SonoraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (playbackState.playingFromOfflineVault) {
                            Icon(
                                imageVector = Icons.Rounded.OfflinePin,
                                contentDescription = "Playing from offline vault",
                                tint = SonoraTeal,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        Text(
                            text = track.artist,
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (equalizerSettings.isModifiedFromFlat) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SonoraEmerald.copy(alpha = 0.16f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Equalizer,
                                        contentDescription = "Equalizer ${equalizerSettings.preset.label} active",
                                        tint = SonoraEmerald,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = equalizerSettings.preset.label,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SonoraEmerald,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                        val sleepMs = playbackState.sleepTimerRemainingMs
                        if (sleepMs != null && sleepMs > 0L) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Surface(
                                color = SonoraViolet.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Bedtime,
                                        contentDescription = "Sleep timer active",
                                        tint = SonoraViolet,
                                        modifier = Modifier.size(10.dp)
                                    )
                                    Text(
                                        text = SleepTimerProtocol.formatCountdown(sleepMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SonoraViolet
                                    )
                                }
                            }
                        }
                    }
                }

                IconButton(
                    onClick = onToggleLike,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_like")
                ) {
                    Icon(
                        imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                        contentDescription = if (isLiked) "Unlike current track" else "Like current track",
                        tint = if (isLiked) SonoraEmerald else SonoraTextSecondary
                    )
                }

                IconButton(
                    onClick = onTogglePlayPause,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("mini_player_play_pause")
                ) {
                    Icon(
                        imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                        tint = SonoraTextPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                color = SonoraEmerald,
                trackColor = SonoraOutline,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp)
                    .padding(horizontal = 10.dp)
                    .clip(RoundedCornerShape(50))
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NowPlayingSheet(
    playbackState: PlaybackState,
    equalizerSettings: EqualizerSettings,
    isLiked: Boolean,
    offlineEntity: OfflineTrackEntity?,
    onDismiss: () -> Unit,
    onTogglePlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onSkipNext: () -> Unit,
    onSkipPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleLike: () -> Unit,
    onDownloadClick: () -> Unit,
    onRemoveDownloadClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onShareTrackClick: () -> Unit,
    onTranscribeTrackClick: () -> Unit,
    onOpenEqualizerClick: () -> Unit,
    onOpenSleepTimerClick: () -> Unit,
    onSelectQueueTrack: (Track) -> Unit
) {
    val track = playbackState.currentTrack ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showQueue by remember { mutableStateOf(false) }

    val accentColor = Color(track.accentHex)
    val downloadStatus = offlineEntity?.status ?: OfflineDownloadStatus.NONE

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SonoraBg,
        dragHandle = null
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            accentColor.copy(alpha = 0.24f),
                            SonoraSurface,
                            SonoraBg
                        )
                    )
                )
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("now_playing_collapse")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.KeyboardArrowDown,
                            contentDescription = "Collapse player",
                            tint = SonoraTextPrimary,
                            modifier = Modifier.size(30.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (playbackState.playingFromOfflineVault) "OFFLINE VAULT" else "STREAMING",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (playbackState.playingFromOfflineVault) SonoraTeal else SonoraEmerald,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = track.album,
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary
                        )
                    }

                    Row {
                        IconButton(
                            onClick = onOpenSleepTimerClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_sleep_timer_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Bedtime,
                                contentDescription = "Open sleep timer",
                                tint = if ((playbackState.sleepTimerRemainingMs ?: 0L) > 0L) {
                                    SonoraViolet
                                } else {
                                    SonoraTextPrimary
                                }
                            )
                        }
                        IconButton(
                            onClick = onOpenEqualizerClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_eq_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Equalizer,
                                contentDescription = "Open equalizer",
                                tint = if (equalizerSettings.isModifiedFromFlat) {
                                    SonoraEmerald
                                } else {
                                    SonoraTextPrimary
                                }
                            )
                        }
                        IconButton(
                            onClick = onShareTrackClick,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_share_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share track",
                                tint = SonoraTextPrimary
                            )
                        }
                        IconButton(
                            onClick = { showQueue = !showQueue },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_queue_toggle")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                                contentDescription = "Toggle queue",
                                tint = if (showQueue) SonoraEmerald else SonoraTextPrimary
                            )
                        }
                    }
                }

                if (showQueue) {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = 12.dp)
                    ) {
                        Text(
                            text = "Up Next (${playbackState.queue.size})",
                            style = MaterialTheme.typography.titleLarge,
                            color = SonoraTextPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                        LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            items(playbackState.queue, key = { it.id }) { qTrack ->
                                val active = qTrack.id == track.id
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(
                                            if (active) SonoraEmerald.copy(alpha = 0.14f)
                                            else SonoraSurfaceElevated.copy(alpha = 0.6f)
                                        )
                                        .clickable { onSelectQueueTrack(qTrack) }
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    PlaylistCoverArt(
                                        coverKey = qTrack.coverKey,
                                        contentDescription = qTrack.title,
                                        modifier = Modifier
                                            .size(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = qTrack.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            color = if (active) SonoraEmerald else SonoraTextPrimary
                                        )
                                        Text(
                                            text = qTrack.artist,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = SonoraTextSecondary
                                        )
                                    }
                                    Text(
                                        text = formatDurationMs(qTrack.durationMs),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SonoraTextMuted
                                    )
                                }
                            }
                        }
                    }
                } else {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(0.82f)
                                .height(250.dp)
                                .clip(RoundedCornerShape(26.dp))
                                .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(26.dp))
                        ) {
                            PlaylistCoverArt(
                                coverKey = track.coverKey,
                                contentDescription = "${track.title} album art",
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(18.dp))

                        AudioWaveformVisualizer(
                            isPlaying = playbackState.isPlaying,
                            accentColor = accentColor,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(34.dp)
                        )
                    }
                }

                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = SonoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${track.artist} • ${track.genre} • ${track.bpm} BPM",
                                style = MaterialTheme.typography.bodyMedium,
                                color = SonoraTextSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        IconButton(
                            onClick = onToggleLike,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_like")
                        ) {
                            Icon(
                                imageVector = if (isLiked) Icons.Rounded.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = "Toggle favorite",
                                tint = if (isLiked) SonoraEmerald else SonoraTextSecondary,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    val duration = playbackState.durationMs.coerceAtLeast(1L)
                    Slider(
                        value = playbackState.positionMs.toFloat().coerceIn(0f, duration.toFloat()),
                        onValueChange = { onSeekTo(it.toLong()) },
                        valueRange = 0f..duration.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = SonoraTextPrimary,
                            activeTrackColor = SonoraEmerald,
                            inactiveTrackColor = SonoraOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("now_playing_slider")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formatDurationMs(playbackState.positionMs),
                            style = MaterialTheme.typography.labelMedium,
                            color = SonoraTextSecondary
                        )
                        Surface(
                            color = if (equalizerSettings.isModifiedFromFlat) {
                                SonoraEmerald.copy(alpha = 0.16f)
                            } else {
                                SonoraSurfaceElevated
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .border(
                                    1.dp,
                                    if (equalizerSettings.isModifiedFromFlat) {
                                        SonoraEmerald.copy(alpha = 0.5f)
                                    } else {
                                        SonoraOutline
                                    },
                                    RoundedCornerShape(50)
                                )
                                .clickable(onClick = onOpenEqualizerClick)
                                .testTag("now_playing_eq_badge")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Equalizer,
                                    contentDescription = "Equalizer preset",
                                    tint = if (equalizerSettings.isModifiedFromFlat) {
                                        SonoraEmerald
                                    } else {
                                        SonoraTextMuted
                                    },
                                    modifier = Modifier.size(13.dp)
                                )
                                Text(
                                    text = if (equalizerSettings.enabled) {
                                        "${track.bitrateLabel} • EQ ${equalizerSettings.preset.label}"
                                    } else {
                                        "${track.bitrateLabel} • EQ Off"
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (equalizerSettings.isModifiedFromFlat) {
                                        SonoraEmerald
                                    } else {
                                        SonoraTextMuted
                                    }
                                )
                            }
                        }
                        Text(
                            text = formatDurationMs(duration),
                            style = MaterialTheme.typography.labelMedium,
                            color = SonoraTextSecondary
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = onToggleShuffle,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_shuffle")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = if (playbackState.shuffleEnabled) SonoraEmerald else SonoraTextSecondary
                            )
                        }

                        IconButton(
                            onClick = onSkipPrevious,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_previous")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous track",
                                tint = SonoraTextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        Box(
                            modifier = Modifier
                                .size(68.dp)
                                .clip(CircleShape)
                                .background(SonoraEmerald)
                                .clickable(onClick = onTogglePlayPause)
                                .testTag("now_playing_play_pause"),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (playbackState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playbackState.isPlaying) "Pause" else "Play",
                                tint = SonoraBg,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onSkipNext,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_next")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.SkipNext,
                                contentDescription = "Next track",
                                tint = SonoraTextPrimary,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = onCycleRepeat,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("now_playing_repeat")
                        ) {
                            Icon(
                                imageVector = if (playbackState.repeatMode == Player.REPEAT_MODE_ONE) {
                                    Icons.Rounded.RepeatOne
                                } else {
                                    Icons.Rounded.Repeat
                                },
                                contentDescription = "Repeat mode",
                                tint = if (playbackState.repeatMode != Player.REPEAT_MODE_OFF) {
                                    SonoraEmerald
                                } else {
                                    SonoraTextSecondary
                                }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = SonoraOutline.copy(alpha = 0.5f))
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = SonoraSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    when (downloadStatus) {
                                        OfflineDownloadStatus.DOWNLOADED -> onRemoveDownloadClick()
                                        OfflineDownloadStatus.DOWNLOADING -> Unit
                                        else -> onDownloadClick()
                                    }
                                }
                                .testTag("now_playing_offline_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = if (downloadStatus == OfflineDownloadStatus.DOWNLOADED) {
                                        Icons.Rounded.CheckCircle
                                    } else {
                                        Icons.Rounded.DownloadForOffline
                                    },
                                    contentDescription = "Offline status",
                                    tint = if (downloadStatus == OfflineDownloadStatus.DOWNLOADED) {
                                        SonoraTeal
                                    } else {
                                        SonoraTextPrimary
                                    },
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = when (downloadStatus) {
                                        OfflineDownloadStatus.DOWNLOADED -> "Offline"
                                        OfflineDownloadStatus.DOWNLOADING -> "${offlineEntity?.progressPercent ?: 0}%"
                                        else -> "Download"
                                    },
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonoraTextPrimary
                                )
                            }
                        }

                        Surface(
                            color = SonoraViolet.copy(alpha = 0.16f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    onDismiss()
                                    onTranscribeTrackClick()
                                }
                                .testTag("now_playing_transcribe_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.GraphicEq,
                                    contentDescription = "Transcribe audio feed",
                                    tint = SonoraViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Transcribe",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonoraViolet
                                )
                            }
                        }

                        Surface(
                            color = SonoraSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onAddToPlaylistClick)
                                .testTag("now_playing_add_playlist_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.PlaylistAdd,
                                    contentDescription = "Add to playlist",
                                    tint = SonoraTextPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Playlist",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonoraTextPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AudioWaveformVisualizer(
    isPlaying: Boolean,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "waveform")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wavePhase"
    )

    Canvas(modifier = modifier) {
        val barCount = 28
        val slotWidth = size.width / barCount
        val barWidth = (slotWidth * 0.52f).coerceAtLeast(3f)
        val centerY = size.height / 2f

        for (i in 0 until barCount) {
            val normalized = if (isPlaying) {
                0.3f + 0.68f * ((sin(phase + i * 0.48f) + 1f) * 0.5f)
            } else {
                0.22f + 0.14f * ((sin(i * 0.6f) + 1f) * 0.5f)
            }
            val barHeight = (size.height * normalized).coerceAtLeast(4f)
            val x = i * slotWidth + (slotWidth - barWidth) / 2f
            drawRoundRect(
                color = if (i % 3 == 0) accentColor else SonoraEmerald.copy(alpha = 0.72f),
                topLeft = Offset(x, centerY - barHeight / 2f),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
