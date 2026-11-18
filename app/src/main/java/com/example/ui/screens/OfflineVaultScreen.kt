package com.example.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.catalog.MusicCatalog
import com.example.data.local.status
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.Track
import com.example.player.PlaybackState
import com.example.ui.SonoraHomeData
import com.example.ui.components.TrackRowItem
import com.example.ui.components.formatStorageMb
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary

@Composable
fun OfflineVaultScreen(
    data: SonoraHomeData,
    playbackState: PlaybackState,
    onToggleOfflineMode: (Boolean) -> Unit,
    onDownloadAllCatalog: () -> Unit,
    onClearVault: () -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    val downloaded = data.downloadedTracks
    val availableToCache = MusicCatalog.tracks.filter {
        data.offlineMap[it.id]?.status != OfflineDownloadStatus.DOWNLOADED
    }
    val progressRatio = downloaded.size.toFloat() / MusicCatalog.tracks.size.toFloat()

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
                text = "Offline Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = SonoraTextPrimary
            )
        }

        // Offline Mode Master Switch Card
        item {
            Surface(
                color = if (data.offlineModeEnabled) SonoraTeal.copy(alpha = 0.14f) else SonoraSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (data.offlineModeEnabled) SonoraTeal else SonoraOutline,
                        RoundedCornerShape(20.dp)
                    )
            ) {
                Row(
                    modifier = Modifier.padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(SonoraTeal.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.OfflineBolt,
                            contentDescription = "Offline Mode",
                            tint = SonoraTeal
                        )
                    }
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.offline_mode_label),
                            style = MaterialTheme.typography.titleMedium,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = stringResource(R.string.offline_mode_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Switch(
                        checked = data.offlineModeEnabled,
                        onCheckedChange = onToggleOfflineMode,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SonoraBg,
                            checkedTrackColor = SonoraTeal
                        ),
                        modifier = Modifier.testTag("offline_mode_switch")
                    )
                }
            }
        }

        // Storage Summary Card
        item {
            Surface(
                color = SonoraSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraOutline, RoundedCornerShape(20.dp))
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Storage,
                                contentDescription = "Vault storage",
                                tint = SonoraEmerald
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "${downloaded.size} of ${MusicCatalog.tracks.size} tracks cached",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = "Device Storage Used: ${formatStorageMb(data.totalOfflineBytes)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonoraTeal
                                )
                            }
                        }
                    }

                    LinearProgressIndicator(
                        progress = { progressRatio },
                        color = SonoraTeal,
                        trackColor = SonoraOutline,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(50))
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = onDownloadAllCatalog,
                            enabled = availableToCache.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SonoraEmerald,
                                contentColor = SonoraBg
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("download_all_catalog_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DownloadForOffline,
                                contentDescription = "Download all",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Download All", fontWeight = FontWeight.Bold)
                        }

                        OutlinedButton(
                            onClick = onClearVault,
                            enabled = downloaded.isNotEmpty(),
                            shape = RoundedCornerShape(50),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraCoral),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("clear_vault_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.DeleteSweep,
                                contentDescription = "Clear vault",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Clear Vault")
                        }
                    }
                }
            }
        }

        if (downloaded.isNotEmpty()) {
            item {
                Text(
                    text = "Ready for Offline Playback (${downloaded.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = SonoraTeal
                )
            }
            items(downloaded, key = { "offline_${it.id}" }) { track ->
                TrackRowItem(
                    track = track,
                    isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                    isLiked = track.id in data.likedTrackIds,
                    offlineEntity = data.offlineMap[track.id],
                    offlineModeEnabled = data.offlineModeEnabled,
                    onPlayClick = { onPlayTrack(track, downloaded) },
                    onToggleLike = { onToggleLike(track) },
                    onDownloadClick = { onDownloadTrack(track) },
                    onRemoveDownloadClick = { onRemoveDownload(track) },
                    onAddToPlaylistClick = { onAddToPlaylist(track) },
                    onShareClick = { onShareTrack(track) }
                )
            }
        }

        if (availableToCache.isNotEmpty()) {
            item {
                Text(
                    text = "Available to Cache (${availableToCache.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = SonoraTextPrimary
                )
            }
            items(availableToCache, key = { "uncached_${it.id}" }) { track ->
                TrackRowItem(
                    track = track,
                    isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                    isLiked = track.id in data.likedTrackIds,
                    offlineEntity = data.offlineMap[track.id],
                    offlineModeEnabled = data.offlineModeEnabled,
                    onPlayClick = { onPlayTrack(track, MusicCatalog.tracks) },
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
