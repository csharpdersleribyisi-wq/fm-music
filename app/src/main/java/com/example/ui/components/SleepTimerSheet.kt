package com.example.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.player.PlaybackState
import com.example.player.SleepTimerProtocol
import com.example.ui.theme.SonoraBg
import com.example.ui.theme.SonoraCoral
import com.example.ui.theme.SonoraEmerald
import com.example.ui.theme.SonoraOutline
import com.example.ui.theme.SonoraSurface
import com.example.ui.theme.SonoraSurfaceElevated
import com.example.ui.theme.SonoraTeal
import com.example.ui.theme.SonoraTextMuted
import com.example.ui.theme.SonoraTextPrimary
import com.example.ui.theme.SonoraTextSecondary
import com.example.ui.theme.SonoraViolet
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    playbackState: PlaybackState,
    onDismiss: () -> Unit,
    onStartTimer: (minutes: Int, fadeOut: Boolean) -> Unit,
    onExtendTimer: (extraMinutes: Int) -> Unit,
    onCancelTimer: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val remainingMs = playbackState.sleepTimerRemainingMs
    val totalMs = playbackState.sleepTimerTotalMs
    val isActive = remainingMs != null && remainingMs > 0L

    var selectedMinutes by remember {
        mutableIntStateOf(
            ((totalMs ?: (30L * 60_000L)) / 60_000L).toInt().coerceIn(1, 120)
        )
    }
    var fadeOutEnabled by remember { mutableStateOf(playbackState.sleepTimerFadeOut) }

    val progressFraction = if (isActive && totalMs != null && totalMs > 0L) {
        (remainingMs!!.toFloat() / totalMs.toFloat()).coerceIn(0f, 1f)
    } else {
        (selectedMinutes / 120f).coerceIn(0.05f, 1f)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = SonoraSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(
                                        SonoraViolet.copy(alpha = 0.28f),
                                        SonoraTeal.copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                if (isActive) SonoraViolet else SonoraOutline,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Sleep Timer",
                            tint = if (isActive) SonoraViolet else SonoraTextPrimary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Sleep Timer",
                            style = MaterialTheme.typography.titleLarge,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = if (isActive) {
                                "Playback pauses automatically in ${SleepTimerProtocol.formatCountdown(remainingMs!!)}"
                            } else {
                                "Set a duration to pause music automatically"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isActive) SonoraViolet else SonoraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Circular Countdown Dial Card
            Surface(
                color = SonoraBg,
                shape = RoundedCornerShape(22.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (isActive) SonoraViolet.copy(alpha = 0.5f) else SonoraOutline,
                        RoundedCornerShape(22.dp)
                    )
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 20.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.size(164.dp)) {
                        val strokeW = 10.dp.toPx()
                        val pad = strokeW / 2f
                        val arcSize = Size(size.width - strokeW, size.height - strokeW)

                        drawArc(
                            color = SonoraSurfaceElevated,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )

                        drawArc(
                            brush = Brush.sweepGradient(
                                colors = if (isActive) {
                                    listOf(SonoraViolet, SonoraTeal, SonoraEmerald, SonoraViolet)
                                } else {
                                    listOf(SonoraTeal, SonoraEmerald, SonoraTeal)
                                }
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction,
                            useCenter = false,
                            topLeft = Offset(pad, pad),
                            size = arcSize,
                            style = Stroke(width = strokeW, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = if (isActive) "PAUSING IN" else "DURATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isActive) SonoraViolet else SonoraTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isActive) {
                                SleepTimerProtocol.formatCountdown(remainingMs!!)
                            } else {
                                SleepTimerProtocol.formatCountdown(selectedMinutes * 60_000L)
                            },
                            style = MaterialTheme.typography.headlineLarge,
                            color = SonoraTextPrimary,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.testTag("sleep_timer_countdown_text")
                        )
                        Text(
                            text = if (isActive) {
                                if (playbackState.sleepTimerFadeOut) "With gentle fade-out" else "Instant pause"
                            } else {
                                "$selectedMinutes minutes"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = SonoraTextSecondary
                        )
                    }
                }
            }

            // Quick Duration Presets
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Quick Durations",
                    style = MaterialTheme.typography.labelLarge,
                    color = SonoraTextSecondary
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(SleepTimerProtocol.PRESET_MINUTES, key = { it }) { mins ->
                        val selected = selectedMinutes == mins
                        Surface(
                            color = if (selected) {
                                SonoraViolet.copy(alpha = 0.22f)
                            } else {
                                SonoraSurfaceElevated
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (selected) SonoraViolet else SonoraOutline,
                                    RoundedCornerShape(50)
                                )
                                .clip(RoundedCornerShape(50))
                                .clickable {
                                    selectedMinutes = mins
                                }
                                .testTag("sleep_preset_$mins")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Timer,
                                    contentDescription = "$mins minutes",
                                    tint = if (selected) SonoraViolet else SonoraTextSecondary,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = "$mins min",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) SonoraViolet else SonoraTextPrimary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Custom Duration Slider (1 - 120 minutes)
            Surface(
                color = SonoraSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraOutline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Custom Duration",
                            style = MaterialTheme.typography.titleSmall,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "$selectedMinutes min",
                            style = MaterialTheme.typography.labelLarge,
                            color = SonoraEmerald,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Slider(
                        value = selectedMinutes.toFloat(),
                        onValueChange = { selectedMinutes = it.roundToInt().coerceIn(1, 120) },
                        valueRange = 1f..120f,
                        colors = SliderDefaults.colors(
                            thumbColor = SonoraViolet,
                            activeTrackColor = SonoraViolet,
                            inactiveTrackColor = SonoraOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sleep_custom_slider")
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("1 min", style = MaterialTheme.typography.labelSmall, color = SonoraTextMuted)
                        Text("60 min", style = MaterialTheme.typography.labelSmall, color = SonoraTextMuted)
                        Text("120 min", style = MaterialTheme.typography.labelSmall, color = SonoraTextMuted)
                    }
                }
            }

            // Gentle Fade-out Toggle
            Surface(
                color = SonoraSurfaceElevated,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraOutline, RoundedCornerShape(16.dp))
                    .clip(RoundedCornerShape(16.dp))
                    .clickable { fadeOutEnabled = !fadeOutEnabled }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gentle Fade-Out",
                            style = MaterialTheme.typography.titleSmall,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "Smoothly lower volume over the last 3 seconds before pausing",
                            style = MaterialTheme.typography.bodySmall,
                            color = SonoraTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Switch(
                        checked = fadeOutEnabled,
                        onCheckedChange = { fadeOutEnabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SonoraBg,
                            checkedTrackColor = SonoraViolet
                        ),
                        modifier = Modifier.testTag("sleep_fade_switch")
                    )
                }
            }

            // Primary Action Buttons
            if (isActive) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = { onExtendTimer(5) },
                        shape = RoundedCornerShape(50),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("sleep_add_5m_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Add 5 minutes",
                            tint = SonoraTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("+5 min", color = SonoraTeal, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = {
                            onCancelTimer()
                            onDismiss()
                        },
                        shape = RoundedCornerShape(50),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SonoraCoral),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("sleep_cancel_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Cancel timer",
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Turn Off", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Button(
                onClick = {
                    onStartTimer(selectedMinutes, fadeOutEnabled)
                    onDismiss()
                },
                shape = RoundedCornerShape(50),
                colors = ButtonDefaults.buttonColors(
                    containerColor = SonoraViolet,
                    contentColor = SonoraTextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("sleep_start_btn")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Bedtime,
                    contentDescription = "Start sleep timer",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isActive) {
                        "Update Timer to $selectedMinutes min"
                    } else {
                        "Start Sleep Timer ($selectedMinutes min)"
                    },
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(6.dp))
        }
    }
}
