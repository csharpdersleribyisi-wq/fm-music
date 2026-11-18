package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.DownloadForOffline
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.OfflineBolt
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.ai.AiForYouInsights
import com.example.data.catalog.MusicCatalog
import com.example.data.local.status
import com.example.data.model.OfflineDownloadStatus
import com.example.data.model.PersonalizedMix
import com.example.data.model.Track
import com.example.data.model.UiState
import com.example.data.model.UserPlaylist
import com.example.player.EqualizerSettings
import com.example.player.PlaybackState
import com.example.ui.SonoraHomeData
import com.example.ui.components.PlaylistCoverArt
import com.example.ui.components.TrackRowItem
import com.example.ui.theme.SonoraAmber
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraSurfaceHigh
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HomeScreen(
    data: SonoraHomeData,
    playbackState: PlaybackState,
    equalizerSettings: EqualizerSettings,
    aiInsightsState: UiState<AiForYouInsights>?,
    onRefreshAiInsights: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenTasteProfile: () -> Unit,
    onToggleOfflineMode: (Boolean) -> Unit,
    onPlayTrack: (Track, List<Track>) -> Unit,
    onToggleLike: (Track) -> Unit,
    onDownloadTrack: (Track) -> Unit,
    onRemoveDownload: (Track) -> Unit,
    onDownloadTracks: (List<Track>, String) -> Unit,
    onAddToPlaylist: (Track) -> Unit,
    onShareTrack: (Track) -> Unit,
    onOpenMix: (PersonalizedMix) -> Unit,
    onSaveMix: (PersonalizedMix) -> Unit,
    onOpenPlaylist: (UserPlaylist) -> Unit,
    onOpenLikedSongs: () -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    val displayName = data.profile?.displayName?.ifBlank { "Listener" } ?: "Listener"
    val usernameHandle = data.profile?.username?.takeIf { it.isNotBlank() }?.let { "@$it" }
    val heroMix = data.personalizedMixes.firstOrNull()
    val secondaryMixes = data.personalizedMixes.drop(1)
    val recentOrFeatured = data.recentlyPlayedTracks.ifEmpty { MusicCatalog.tracks.take(6) }
    val forYouTopTracks = data.forYouRecommendations.take(5)
    val forYouQueue = forYouTopTracks.map { it.track }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = bottomContentPadding.calculateBottomPadding() + 16.dp
        ),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Header Bar
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (usernameHandle != null) "FOR YOU • $usernameHandle" else "FOR YOU • SONORA",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraEmerald,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Hey, $displayName",
                        style = MaterialTheme.typography.headlineMedium,
                        color = SonoraTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = if (data.offlineModeEnabled) {
                            SonoraTeal.copy(alpha = 0.2f)
                        } else {
                            SonoraSurfaceElevated
                        },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .border(
                                1.dp,
                                if (data.offlineModeEnabled) SonoraTeal else SonoraOutline,
                                RoundedCornerShape(50)
                            )
                            .clickable { onToggleOfflineMode(!data.offlineModeEnabled) }
                            .testTag("home_offline_pill")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.OfflineBolt,
                                contentDescription = "Offline mode toggle",
                                tint = if (data.offlineModeEnabled) SonoraTeal else SonoraTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (data.offlineModeEnabled) "Offline On" else "Online",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (data.offlineModeEnabled) SonoraTeal else SonoraTextPrimary
                            )
                        }
                    }

                    IconButton(
                        onClick = onOpenEqualizer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(
                                if (equalizerSettings.isModifiedFromFlat) {
                                    SonoraEmerald.copy(alpha = 0.2f)
                                } else {
                                    SonoraSurfaceElevated
                                }
                            )
                            .border(
                                1.dp,
                                if (equalizerSettings.isModifiedFromFlat) SonoraEmerald else SonoraOutline,
                                CircleShape
                            )
                            .testTag("home_eq_button")
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
                        onClick = onOpenTasteProfile,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .clip(CircleShape)
                            .background(SonoraSurfaceElevated)
                            .testTag("home_profile_button")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = "Profile and taste preferences",
                            tint = SonoraTextPrimary
                        )
                    }
                }
            }
        }

        // Hero Personalized Mix Card
        if (heroMix != null) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(226.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .border(1.dp, SonoraEmerald.copy(alpha = 0.35f), RoundedCornerShape(24.dp))
                        .clickable { onOpenMix(heroMix) }
                        .testTag("home_hero_mix")
                ) {
                    PlaylistCoverArt(
                        coverKey = heroMix.coverKey,
                        contentDescription = heroMix.title,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        SonoraBg.copy(alpha = 0.25f),
                                        SonoraBg.copy(alpha = 0.88f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                color = SonoraEmerald.copy(alpha = 0.2f),
                                shape = RoundedCornerShape(50)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AutoAwesome,
                                        contentDescription = "Personalized",
                                        tint = SonoraEmerald,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "${heroMix.matchPercent}% MATCH FOR YOU",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SonoraEmerald,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            IconButton(
                                onClick = onOpenTasteProfile,
                                modifier = Modifier.minimumInteractiveComponentSize()
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Tune,
                                    contentDescription = "Tune recommendations",
                                    tint = SonoraTextPrimary
                                )
                            }
                        }

                        Column {
                            Text(
                                text = heroMix.title,
                                style = MaterialTheme.typography.headlineSmall,
                                color = SonoraTextPrimary
                            )
                            Text(
                                text = "${heroMix.subtitle} • ${heroMix.reason}",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Button(
                                    onClick = {
                                        heroMix.tracks.firstOrNull()?.let { first ->
                                            onPlayTrack(first, heroMix.tracks)
                                        }
                                    },
                                    shape = RoundedCornerShape(50),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = SonoraEmerald,
                                        contentColor = SonoraBg
                                    ),
                                    contentPadding = PaddingValues(horizontal = 18.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("hero_play_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play mix",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Play Mix", fontWeight = FontWeight.Bold)
                                }

                                Surface(
                                    color = SonoraSurfaceHigh.copy(alpha = 0.85f),
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onSaveMix(heroMix) }
                                        .testTag("hero_save_mix_button")
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.BookmarkAdd,
                                            contentDescription = "Save mix to playlists",
                                            tint = SonoraTextPrimary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Save",
                                            style = MaterialTheme.typography.labelLarge,
                                            color = SonoraTextPrimary
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = { onDownloadTracks(heroMix.tracks, heroMix.title) },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .clip(CircleShape)
                                        .background(SonoraSurfaceHigh.copy(alpha = 0.85f))
                                        .testTag("hero_download_mix_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.DownloadForOffline,
                                        contentDescription = "Download daily mix offline",
                                        tint = SonoraTeal
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Shortcuts Row (Liked Songs + Top Saved Playlist)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    color = SonoraSurfaceElevated,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(onClick = onOpenLikedSongs)
                        .testTag("home_liked_shortcut")
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    Brush.linearGradient(
                                        listOf(SonoraEmerald, SonoraTeal)
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Favorite,
                                contentDescription = "Liked Songs",
                                tint = SonoraBg
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Liked Songs",
                                style = MaterialTheme.typography.titleSmall,
                                color = SonoraTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${data.likedTrackIds.size} saved",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }
                    }
                }

                val firstPlaylist = data.playlists.firstOrNull()
                if (firstPlaylist != null) {
                    Surface(
                        color = SonoraSurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { onOpenPlaylist(firstPlaylist) }
                            .testTag("home_top_playlist_shortcut")
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            PlaylistCoverArt(
                                coverKey = firstPlaylist.coverKey,
                                contentDescription = firstPlaylist.title,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = firstPlaylist.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SonoraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${firstPlaylist.trackIds.size} tracks",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Dedicated 'For You' Listening Habit Engine & AI Taste Analyzer
        item {
            Surface(
                color = SonoraSurface,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraViolet.copy(alpha = 0.45f), RoundedCornerShape(20.dp))
                    .testTag("for_you_engine_card")
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "FOR YOU RECOMMENDATION ENGINE",
                                style = MaterialTheme.typography.labelSmall,
                                color = SonoraViolet,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Listening Habits & Taste Radar",
                                style = MaterialTheme.typography.titleMedium,
                                color = SonoraTextPrimary
                            )
                        }
                        Surface(
                            color = SonoraViolet.copy(alpha = 0.2f),
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onRefreshAiInsights)
                                .testTag("refresh_ai_insights_btn")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Analyze taste with AI",
                                    tint = SonoraViolet,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "AI Taste",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = SonoraViolet,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Signal summary pills from genres, artists, frequent plays, likes & playlists
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        data.habitSummary.topGenres.take(3).forEach { (genre, _) ->
                            HabitSignalChip(label = genre, accent = SonoraEmerald)
                        }
                        data.habitSummary.topArtists.take(2).forEach { (artist, _) ->
                            HabitSignalChip(label = artist, accent = SonoraTeal)
                        }
                        data.habitSummary.mostPlayedTracks.firstOrNull()?.let { (track, count) ->
                            HabitSignalChip(
                                label = "Top Play: ${track.title} (${count}x)",
                                accent = SonoraAmber
                            )
                        }
                        HabitSignalChip(
                            label = "${data.habitSummary.likedCount} liked • ${data.habitSummary.playlistTrackCount} in playlists",
                            accent = SonoraViolet
                        )
                    }

                    when (aiInsightsState) {
                        null -> {
                            Text(
                                text = "Tap \"AI Taste\" to analyze your played genres, favorite artists, liked songs, and playlists with Gemini.",
                                style = MaterialTheme.typography.bodySmall,
                                color = SonoraTextSecondary
                            )
                        }

                        UiState.Loading -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    color = SonoraViolet,
                                    strokeWidth = 2.dp
                                )
                                Text(
                                    text = "Analyzing your genres, artists, frequent plays & playlists…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary
                                )
                            }
                        }

                        is UiState.Error -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = "AI insight error",
                                    tint = SonoraCoral,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = aiInsightsState.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraCoral
                                )
                            }
                        }

                        is UiState.Success -> {
                            val insights = aiInsightsState.data
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(SonoraSurfaceElevated)
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = insights.headline,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = SonoraEmerald,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = insights.tasteSummary,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextPrimary
                                )
                                insights.picks.forEach { pick ->
                                    MusicCatalog.getTrack(pick.trackId)?.let { pickedTrack ->
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(SonoraSurface)
                                                .clickable {
                                                    onPlayTrack(
                                                        pickedTrack,
                                                        insights.picks.mapNotNull { MusicCatalog.getTrack(it.trackId) }
                                                    )
                                                }
                                                .padding(horizontal = 10.dp, vertical = 8.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.PlayArrow,
                                                contentDescription = "Play ${pickedTrack.title}",
                                                tint = SonoraEmerald,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = "${pickedTrack.title} • ${pickedTrack.artist}",
                                                    style = MaterialTheme.typography.labelLarge,
                                                    color = SonoraTextPrimary
                                                )
                                                Text(
                                                    text = pick.reason,
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = SonoraTextSecondary
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Personalized 'For You' Track Suggestions
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "For You • Fresh Picks",
                    style = MaterialTheme.typography.titleLarge,
                    color = SonoraTextPrimary
                )
                Text(
                    text = "Habit-scored",
                    style = MaterialTheme.typography.labelMedium,
                    color = SonoraEmerald
                )
            }
        }

        items(forYouTopTracks, key = { "foryou_${it.track.id}" }) { rec ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(SonoraSurface)
                    .border(1.dp, SonoraOutline.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                TrackRowItem(
                    track = rec.track,
                    isPlayingCurrent = playbackState.currentTrack?.id == rec.track.id,
                    isLiked = rec.track.id in data.likedTrackIds,
                    offlineEntity = data.offlineMap[rec.track.id],
                    offlineModeEnabled = data.offlineModeEnabled,
                    onPlayClick = { onPlayTrack(rec.track, forYouQueue) },
                    onToggleLike = { onToggleLike(rec.track) },
                    onDownloadClick = { onDownloadTrack(rec.track) },
                    onRemoveDownloadClick = { onRemoveDownload(rec.track) },
                    onAddToPlaylistClick = { onAddToPlaylist(rec.track) },
                    onShareClick = { onShareTrack(rec.track) }
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = rec.reason,
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${rec.matchPercent}% match",
                        style = MaterialTheme.typography.labelSmall,
                        color = SonoraEmerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Personalized Mixes Carousel
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Curated Daily Mixes",
                        style = MaterialTheme.typography.titleLarge,
                        color = SonoraTextPrimary
                    )
                    Text(
                        text = data.profile?.preferredMood?.let { "Mood: $it" } ?: "Adaptive",
                        style = MaterialTheme.typography.labelMedium,
                        color = SonoraTeal
                    )
                }

                LazyRow(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    items(secondaryMixes, key = { it.id }) { mix ->
                        val cachedCount = mix.tracks.count {
                            data.offlineMap[it.id]?.status == OfflineDownloadStatus.DOWNLOADED
                        }
                        Surface(
                            color = SonoraSurface,
                            shape = RoundedCornerShape(18.dp),
                            modifier = Modifier
                                .width(196.dp)
                                .border(1.dp, SonoraOutline, RoundedCornerShape(18.dp))
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { onOpenMix(mix) }
                                .testTag("mix_card_${mix.id}")
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(142.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                ) {
                                    PlaylistCoverArt(
                                        coverKey = mix.coverKey,
                                        contentDescription = mix.title,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                    Surface(
                                        color = SonoraBg.copy(alpha = 0.78f),
                                        shape = RoundedCornerShape(50),
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(8.dp)
                                    ) {
                                        Text(
                                            text = "${mix.matchPercent}%",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = SonoraEmerald,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(10.dp))
                                Text(
                                    text = mix.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SonoraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = mix.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = SonoraTextSecondary,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "$cachedCount/${mix.tracks.size} cached offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (cachedCount == mix.tracks.size) SonoraTeal else SonoraTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Jump Back In / Recent Tracks
        item {
            Text(
                text = if (data.recentlyPlayedTracks.isNotEmpty()) "Jump Back In" else "Featured Soundscapes",
                style = MaterialTheme.typography.titleLarge,
                color = SonoraTextPrimary
            )
        }

        items(recentOrFeatured, key = { it.id }) { track ->
            TrackRowItem(
                track = track,
                isPlayingCurrent = playbackState.currentTrack?.id == track.id,
                isLiked = track.id in data.likedTrackIds,
                offlineEntity = data.offlineMap[track.id],
                offlineModeEnabled = data.offlineModeEnabled,
                onPlayClick = { onPlayTrack(track, recentOrFeatured) },
                onToggleLike = { onToggleLike(track) },
                onDownloadClick = { onDownloadTrack(track) },
                onRemoveDownloadClick = { onRemoveDownload(track) },
                onAddToPlaylistClick = { onAddToPlaylist(track) },
                onShareClick = { onShareTrack(track) }
            )
        }
    }
}

@Composable
private fun HabitSignalChip(label: String, accent: androidx.compose.ui.graphics.Color) {
    Surface(
        color = accent.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        modifier = Modifier.border(1.dp, accent.copy(alpha = 0.35f), RoundedCornerShape(50))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = accent,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
        )
    }
}
