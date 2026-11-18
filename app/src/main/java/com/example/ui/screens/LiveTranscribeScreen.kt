package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.data.catalog.MusicCatalog
import com.example.data.model.Track
import com.example.data.model.TranscriptSegment
import com.example.player.PlaybackState
import com.example.ui.theme.SonoraAmber
import com.example.ui.theme.SonoraBg
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
fun LiveTranscribeScreen(
    playbackState: PlaybackState,
    isRunning: Boolean,
    sourceLabel: String,
    liveMicLevel: Float,
    segments: List<TranscriptSegment>,
    errorMessage: String?,
    onStartLiveMic: () -> Unit,
    onTranscribeTrack: (Track) -> Unit,
    onStop: () -> Unit,
    onClear: () -> Unit,
    onNotice: (String) -> Unit,
    bottomContentPadding: PaddingValues = PaddingValues(bottom = 16.dp)
) {
    val context = LocalContext.current
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current

    var permissionDeniedNote by remember { mutableStateOf(false) }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            permissionDeniedNote = false
            onStartLiveMic()
        } else {
            permissionDeniedNote = true
            onNotice("Microphone permission is required for live ambient feed transcription.")
        }
    }

    fun requestOrStartMic() {
        val hasPerm = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
        if (hasPerm) {
            permissionDeniedNote = false
            onStartLiveMic()
        } else {
            micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    val activeTrack = playbackState.currentTrack ?: MusicCatalog.tracks.first()

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
            Column {
                Text(
                    text = "LIVE AUDIO TRANSCRIPTION",
                    style = MaterialTheme.typography.labelSmall,
                    color = SonoraViolet,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Real-Time Feed Captions",
                    style = MaterialTheme.typography.headlineMedium,
                    color = SonoraTextPrimary
                )
                Text(
                    text = "Stream live microphone audio or any playing Sonora audio feed for real-time speech, lyrics & sonic cue transcription.",
                    style = MaterialTheme.typography.bodySmall,
                    color = SonoraTextSecondary
                )
            }
        }

        // Live Feed Control Deck
        item {
            Surface(
                color = SonoraSurfaceElevated,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isRunning) SonoraEmerald else SonoraOutline,
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Rounded.GraphicEq,
                                contentDescription = "Audio feed",
                                tint = if (isRunning) SonoraEmerald else SonoraViolet
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = if (isRunning) "LIVE STREAM ACTIVE" else "READY TO TRANSCRIBE",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (isRunning) SonoraEmerald else SonoraTextSecondary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = sourceLabel,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = SonoraTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    LiveMeterBar(
                        level = liveMicLevel,
                        isRunning = isRunning,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        if (isRunning) {
                            Button(
                                onClick = onStop,
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SonoraCoral,
                                    contentColor = SonoraBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("transcribe_stop_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Stop,
                                    contentDescription = "Stop transcription",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Stop Live Feed", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            Button(
                                onClick = { requestOrStartMic() },
                                shape = RoundedCornerShape(50),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SonoraEmerald,
                                    contentColor = SonoraBg
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("transcribe_mic_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Mic,
                                    contentDescription = "Transcribe microphone feed",
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Live Mic Feed", fontWeight = FontWeight.Bold)
                            }

                            OutlinedButton(
                                onClick = { onTranscribeTrack(activeTrack) },
                                shape = RoundedCornerShape(50),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("transcribe_current_track_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.AudioFile,
                                    contentDescription = "Transcribe playing track",
                                    tint = SonoraTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Track Feed",
                                    color = SonoraTeal,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    if (permissionDeniedNote) {
                        Text(
                            text = "Microphone permission was denied. You can still transcribe any music audio feed below or grant Mic permission in Android Settings.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraAmber
                        )
                    }
                    if (!errorMessage.isNullOrBlank()) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraCoral
                        )
                    }
                }
            }
        }

        // Quick Select Any Catalog Audio Feed to Transcribe
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Transcribe Any Audio Stream",
                    style = MaterialTheme.typography.titleMedium,
                    color = SonoraTextPrimary
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(MusicCatalog.tracks, key = { "tr_pick_${it.id}" }) { track ->
                        val isCurrent = playbackState.currentTrack?.id == track.id
                        Surface(
                            color = if (isCurrent) SonoraViolet.copy(alpha = 0.2f) else SonoraSurface,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (isCurrent) SonoraViolet else SonoraOutline,
                                    RoundedCornerShape(14.dp)
                                )
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onTranscribeTrack(track) }
                                .testTag("transcribe_pick_${track.id}")
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(
                                    text = track.title,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = SonoraTextPrimary
                                )
                                Text(
                                    text = "${track.artist} • ${track.bpm} BPM",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = SonoraTextSecondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Transcript Stream Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Live Transcript (${segments.size})",
                    style = MaterialTheme.typography.titleLarge,
                    color = SonoraTextPrimary
                )
                if (segments.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Surface(
                            color = SonoraSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    val fullText = segments.reversed().joinToString("\n\n") {
                                        "[${it.timestampLabel}] ${it.sourceLabel}\n${it.text}"
                                    }
                                    clipboard.setText(AnnotatedString(fullText))
                                    onNotice("Copied full transcript to clipboard")
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = "Copy transcript",
                                    tint = SonoraTeal,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Copy", style = MaterialTheme.typography.labelSmall, color = SonoraTeal)
                            }
                        }
                        Surface(
                            color = SonoraSurfaceElevated,
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .clickable(onClick = onClear)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ClearAll,
                                    contentDescription = "Clear transcript",
                                    tint = SonoraTextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Clear", style = MaterialTheme.typography.labelSmall, color = SonoraTextSecondary)
                            }
                        }
                    }
                }
            }
        }

        if (segments.isEmpty()) {
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
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = "No transcript yet",
                            tint = SonoraTextSecondary,
                            modifier = Modifier.size(34.dp)
                        )
                        Text(
                            text = "No live feed captured yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "Tap \"Live Mic Feed\" for continuous 4-second real-time speech & audio chunks, or choose any track feed above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary
                        )
                    }
                }
            }
        } else {
            items(segments, key = { it.id }) { segment ->
                Surface(
                    color = SonoraSurface,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            1.dp,
                            if (segment.streaming) SonoraEmerald.copy(alpha = 0.6f) else SonoraOutline,
                            RoundedCornerShape(16.dp)
                        )
                        .testTag("transcript_segment_${segment.id}")
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = segment.sourceLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = if (segment.streaming) SonoraEmerald else SonoraTeal,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (segment.streaming) "${segment.timestampLabel} • STREAMING" else segment.timestampLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = SonoraTextSecondary
                            )
                        }
                        Text(
                            text = segment.text,
                            style = MaterialTheme.typography.bodyMedium,
                            color = SonoraTextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveMeterBar(
    level: Float,
    isRunning: Boolean,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val bars = 24
        val slot = size.width / bars
        val barWidth = (slot * 0.56f).coerceAtLeast(3f)
        val centerY = size.height / 2f
        val activeLevel = if (isRunning) level.coerceIn(0.08f, 1f) else 0.08f

        for (i in 0 until bars) {
            val falloff = 1f - kotlin.math.abs(i - bars / 2f) / (bars / 2f)
            val norm = (0.14f + activeLevel * (0.4f + 0.6f * falloff)).coerceIn(0.1f, 1f)
            val h = size.height * norm
            val x = i * slot + (slot - barWidth) / 2f
            drawRoundRect(
                color = if (isRunning) {
                    if (i % 3 == 0) SonoraViolet else SonoraEmerald
                } else {
                    SonoraOutline
                },
                topLeft = Offset(x, centerY - h / 2f),
                size = Size(barWidth, h),
                cornerRadius = CornerRadius(barWidth / 2f, barWidth / 2f)
            )
        }
    }
}
