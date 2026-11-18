package com.example.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.player.EqualizerPreset
import com.example.player.EqualizerSettings
import com.example.ui.theme.SonoraAmber
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
fun EqualizerSheet(
    settings: EqualizerSettings,
    onDismiss: () -> Unit,
    onToggleEnabled: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onUpdateBand: (bandIndex: Int, gainDb: Float) -> Unit,
    onUpdatePreamp: (Float) -> Unit,
    onReset: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val gains = settings.normalizedGains()

    // Animate band transitions smoothly when switching presets
    val animatedGains = gains.mapIndexed { idx, value ->
        val anim by animateFloatAsState(
            targetValue = if (settings.enabled) value else 0f,
            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
            label = "eqBand$idx"
        )
        anim
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
                .padding(horizontal = 20.dp, vertical = 8.dp),
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
                                        SonoraEmerald.copy(alpha = 0.25f),
                                        SonoraTeal.copy(alpha = 0.2f)
                                    )
                                )
                            )
                            .border(
                                1.dp,
                                if (settings.enabled) SonoraEmerald else SonoraOutline,
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Equalizer,
                            contentDescription = "Media3 Equalizer",
                            tint = if (settings.enabled) SonoraEmerald else SonoraTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Media3 Studio Equalizer",
                            style = MaterialTheme.typography.titleLarge,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = if (settings.enabled) {
                                "${settings.preset.label} • ${settings.preset.description}"
                            } else {
                                "Bypassed • Pure PCM pass-through"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (settings.enabled) SonoraEmerald else SonoraTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onReset,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("eq_reset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RestartAlt,
                            contentDescription = "Reset equalizer to Flat",
                            tint = SonoraTextSecondary
                        )
                    }
                    Switch(
                        checked = settings.enabled,
                        onCheckedChange = onToggleEnabled,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = SonoraBg,
                            checkedTrackColor = SonoraEmerald
                        ),
                        modifier = Modifier.testTag("eq_master_switch")
                    )
                }
            }

            // Frequency Response Curve Visualizer
            Surface(
                color = SonoraBg,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        1.dp,
                        if (settings.enabled) SonoraEmerald.copy(alpha = 0.35f) else SonoraOutline,
                        RoundedCornerShape(18.dp)
                    )
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "FREQUENCY RESPONSE",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonoraTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "5-Band Biquad DSP",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (settings.enabled) SonoraTeal else SonoraTextMuted
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    EqualizerResponseCurveCanvas(
                        gainsDb = animatedGains,
                        enabled = settings.enabled,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        EqualizerSettings.BAND_SPECS.forEach { spec ->
                            Text(
                                text = spec.label.replace(" Hz", "").replace(" kHz", "k"),
                                style = MaterialTheme.typography.labelSmall,
                                color = SonoraTextMuted
                            )
                        }
                    }
                }
            }

            // Preset Modes Carousel
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Preset Modes",
                    style = MaterialTheme.typography.labelLarge,
                    color = SonoraTextSecondary
                )
                val visiblePresets = remember(settings.preset) {
                    EqualizerPreset.entries.filter {
                        it != EqualizerPreset.CUSTOM || settings.preset == EqualizerPreset.CUSTOM
                    }
                }
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(end = 8.dp)
                ) {
                    items(visiblePresets, key = { it.name }) { preset ->
                        val selected = settings.preset == preset
                        Surface(
                            color = if (selected) {
                                SonoraEmerald.copy(alpha = 0.2f)
                            } else {
                                SonoraSurfaceElevated
                            },
                            shape = RoundedCornerShape(50),
                            modifier = Modifier
                                .border(
                                    1.dp,
                                    if (selected) SonoraEmerald else SonoraOutline,
                                    RoundedCornerShape(50)
                                )
                                .clip(RoundedCornerShape(50))
                                .clickable(enabled = preset != EqualizerPreset.CUSTOM) {
                                    onSelectPreset(preset)
                                }
                                .testTag("eq_preset_${preset.name.lowercase()}")
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                PresetMiniGlyph(
                                    gains = if (preset == EqualizerPreset.CUSTOM) gains else preset.bandGainsDb,
                                    color = if (selected) SonoraEmerald else SonoraTextSecondary
                                )
                                Text(
                                    text = preset.label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (selected) SonoraEmerald else SonoraTextPrimary,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // 5-Band Custom Vertical Faders
            Surface(
                color = SonoraSurfaceElevated,
                shape = RoundedCornerShape(20.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraOutline, RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Band Gain (±12 dB)",
                            style = MaterialTheme.typography.labelLarge,
                            color = SonoraTextPrimary
                        )
                        Text(
                            text = "Drag faders to customize",
                            style = MaterialTheme.typography.labelSmall,
                            color = SonoraTextSecondary
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        EqualizerSettings.BAND_SPECS.forEach { spec ->
                            val currentGain = gains[spec.index]
                            BandVerticalFader(
                                label = spec.label,
                                roleLabel = spec.roleLabel,
                                gainDb = currentGain,
                                animatedGainDb = animatedGains[spec.index],
                                enabled = settings.enabled,
                                onGainChange = { newGain ->
                                    onUpdateBand(spec.index, newGain)
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("eq_band_${spec.index}")
                            )
                        }
                    }
                }
            }

            // Preamp Output Trim
            Surface(
                color = SonoraSurfaceElevated,
                shape = RoundedCornerShape(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, SonoraOutline, RoundedCornerShape(18.dp))
            ) {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Master Preamp Trim",
                            style = MaterialTheme.typography.titleSmall,
                            color = SonoraTextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (kotlin.math.abs(settings.preampDb) > 0.05f && settings.enabled) {
                                Surface(
                                    color = SonoraSurface,
                                    shape = RoundedCornerShape(50),
                                    modifier = Modifier
                                        .border(1.dp, SonoraOutline, RoundedCornerShape(50))
                                        .clip(RoundedCornerShape(50))
                                        .clickable { onUpdatePreamp(0f) }
                                        .testTag("eq_preamp_zero_btn")
                                ) {
                                    Text(
                                        text = "0 dB",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = SonoraTextSecondary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                            val preampFormatted = formatDbValue(settings.preampDb)
                            Text(
                                text = "$preampFormatted dB",
                                style = MaterialTheme.typography.labelLarge,
                                color = if (settings.preampDb > 0.05f) SonoraAmber else SonoraTeal,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    Slider(
                        value = settings.preampDb,
                        onValueChange = { raw ->
                            val stepped = (raw * 2f).roundToInt() / 2f
                            onUpdatePreamp(stepped)
                        },
                        valueRange = EqualizerSettings.PREAMP_MIN_DB..EqualizerSettings.PREAMP_MAX_DB,
                        enabled = settings.enabled,
                        colors = SliderDefaults.colors(
                            thumbColor = SonoraTeal,
                            activeTrackColor = SonoraTeal,
                            inactiveTrackColor = SonoraOutline
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("eq_preamp_slider")
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun EqualizerResponseCurveCanvas(
    gainsDb: List<Float>,
    enabled: Boolean,
    modifier: Modifier = Modifier
) {
    val lineColor = if (enabled) SonoraEmerald else SonoraTextMuted
    val secondaryLine = if (enabled) SonoraTeal else SonoraTextMuted

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        val padH = 12.dp.toPx()
        val padV = 10.dp.toPx()
        val usableW = (w - padH * 2).coerceAtLeast(1f)
        val usableH = (h - padV * 2).coerceAtLeast(1f)
        val midY = padV + usableH / 2f

        val dashEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)

        // Horizontal reference lines (+12, 0, -12 dB)
        drawLine(
            color = SonoraOutline.copy(alpha = 0.4f),
            start = Offset(padH, padV),
            end = Offset(w - padH, padV),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = SonoraOutline.copy(alpha = 0.85f),
            start = Offset(padH, midY),
            end = Offset(w - padH, midY),
            strokeWidth = 1.2.dp.toPx(),
            pathEffect = dashEffect
        )
        drawLine(
            color = SonoraOutline.copy(alpha = 0.4f),
            start = Offset(padH, h - padV),
            end = Offset(w - padH, h - padV),
            strokeWidth = 1.dp.toPx()
        )

        val points = gainsDb.mapIndexed { idx, gain ->
            val x = padH + (idx.toFloat() / (gainsDb.size - 1).coerceAtLeast(1)) * usableW
            val norm = (gain / EqualizerSettings.MAX_GAIN_DB).coerceIn(-1f, 1f)
            val y = midY - norm * (usableH / 2f)
            Offset(x, y)
        }

        if (points.size >= 2) {
            // Vertical band stems
            points.forEach { pt ->
                drawLine(
                    color = SonoraOutline.copy(alpha = 0.35f),
                    start = Offset(pt.x, padV),
                    end = Offset(pt.x, h - padV),
                    strokeWidth = 1.dp.toPx()
                )
            }

            val curvePath = Path().apply {
                moveTo(points.first().x, points.first().y)
                for (i in 0 until points.lastIndex) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }
            }

            val fillPath = Path().apply {
                addPath(curvePath)
                lineTo(points.last().x, midY)
                lineTo(points.first().x, midY)
                close()
            }

            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        lineColor.copy(alpha = if (enabled) 0.32f else 0.1f),
                        secondaryLine.copy(alpha = 0.04f)
                    )
                )
            )

            drawPath(
                path = curvePath,
                brush = Brush.horizontalGradient(listOf(SonoraViolet, lineColor, secondaryLine)),
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            points.forEach { pt ->
                drawCircle(
                    color = lineColor.copy(alpha = 0.25f),
                    radius = 8.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = SonoraBg,
                    radius = 5.dp.toPx(),
                    center = pt
                )
                drawCircle(
                    color = lineColor,
                    radius = 3.5.dp.toPx(),
                    center = pt
                )
            }
        }
    }
}

@Composable
private fun PresetMiniGlyph(
    gains: List<Float>,
    color: Color
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.Bottom,
        modifier = Modifier.height(14.dp)
    ) {
        gains.forEach { gain ->
            val fraction = ((gain + 12f) / 24f).coerceIn(0.15f, 1f)
            Box(
                modifier = Modifier
                    .width(2.5.dp)
                    .height((14f * fraction).dp)
                    .clip(RoundedCornerShape(50))
                    .background(color)
            )
        }
    }
}

@Composable
private fun BandVerticalFader(
    label: String,
    roleLabel: String,
    gainDb: Float,
    animatedGainDb: Float,
    enabled: Boolean,
    onGainChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val latestOnChange by rememberUpdatedState(onGainChange)
    val density = androidx.compose.ui.platform.LocalDensity.current
    val padYPx = with(density) { 12.dp.toPx() }
    var trackHeightPx by remember { mutableFloatStateOf(1f) }

    val gainColor = when {
        !enabled -> SonoraTextMuted
        gainDb > 0.25f -> SonoraEmerald
        gainDb < -0.25f -> SonoraCoral
        else -> SonoraTextSecondary
    }

    fun yOffsetToGain(yPx: Float): Float {
        val usable = (trackHeightPx - padYPx * 2f).coerceAtLeast(1f)
        val normalized = 1f - ((yPx - padYPx) / usable).coerceIn(0f, 1f)
        val rawGain = EqualizerSettings.MIN_GAIN_DB +
            normalized * (EqualizerSettings.MAX_GAIN_DB - EqualizerSettings.MIN_GAIN_DB)
        // Snap cleanly to 0.5 dB steps for crisp studio control
        return ((rawGain * 2f).roundToInt() / 2f)
            .coerceIn(EqualizerSettings.MIN_GAIN_DB, EqualizerSettings.MAX_GAIN_DB)
    }

    Column(
        modifier = modifier.semantics {
            contentDescription = "$label band set to ${formatDbValue(gainDb)} decibels"
        },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            color = gainColor.copy(alpha = 0.14f),
            shape = RoundedCornerShape(8.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .clickable(enabled = enabled && kotlin.math.abs(gainDb) > 0.05f) {
                    onGainChange(0f)
                }
        ) {
            Text(
                text = formatDbValue(if (enabled) gainDb else 0f),
                style = MaterialTheme.typography.labelSmall,
                color = gainColor,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp)
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(172.dp)
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectTapGestures { offset ->
                        trackHeightPx = size.height.toFloat()
                        latestOnChange(yOffsetToGain(offset.y))
                    }
                }
                .pointerInput(enabled) {
                    if (!enabled) return@pointerInput
                    detectVerticalDragGestures { change, _ ->
                        change.consume()
                        trackHeightPx = size.height.toFloat()
                        latestOnChange(yOffsetToGain(change.position.y))
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .width(40.dp)
                    .height(172.dp)
            ) {
                trackHeightPx = size.height
                val w = size.width
                val h = size.height
                val padY = 12.dp.toPx()
                val usableH = (h - padY * 2).coerceAtLeast(1f)
                val centerX = w / 2f
                val midY = padY + usableH / 2f
                val trackWidth = 6.dp.toPx()

                // Background rail
                drawRoundRect(
                    color = SonoraBg,
                    topLeft = Offset(centerX - trackWidth / 2f, padY),
                    size = Size(trackWidth, usableH),
                    cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                )

                // Zero center tick
                drawLine(
                    color = SonoraOutline,
                    start = Offset(centerX - 11.dp.toPx(), midY),
                    end = Offset(centerX + 11.dp.toPx(), midY),
                    strokeWidth = 1.5.dp.toPx(),
                    cap = StrokeCap.Round
                )

                val norm = (animatedGainDb / EqualizerSettings.MAX_GAIN_DB).coerceIn(-1f, 1f)
                val thumbY = midY - norm * (usableH / 2f)

                val activeColor = when {
                    !enabled -> SonoraTextMuted
                    norm >= 0f -> SonoraEmerald
                    else -> SonoraCoral
                }

                // Active bipolar fill from center 0 dB line to thumb
                val topY = minOf(midY, thumbY)
                val fillH = kotlin.math.abs(thumbY - midY)
                if (fillH > 1f) {
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(centerX - trackWidth / 2f, topY),
                        size = Size(trackWidth, fillH),
                        cornerRadius = CornerRadius(trackWidth / 2f, trackWidth / 2f)
                    )
                }

                // Thumb glow + cap
                drawCircle(
                    color = activeColor.copy(alpha = 0.28f),
                    radius = 14.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = if (enabled) SonoraTextPrimary else SonoraTextMuted,
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
                drawCircle(
                    color = activeColor,
                    radius = 4.dp.toPx(),
                    center = Offset(centerX, thumbY)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = SonoraTextPrimary,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1
            )
            Text(
                text = roleLabel,
                style = MaterialTheme.typography.labelSmall,
                color = SonoraTextMuted,
                maxLines = 1
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = enabled && gainDb > EqualizerSettings.MIN_GAIN_DB) {
                        onGainChange(
                            (gainDb - 1f).coerceIn(
                                EqualizerSettings.MIN_GAIN_DB,
                                EqualizerSettings.MAX_GAIN_DB
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SonoraBg)
                        .border(1.dp, SonoraOutline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Remove,
                        contentDescription = "Decrease $label by 1 dB",
                        tint = if (enabled) SonoraTextSecondary else SonoraTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(enabled = enabled && gainDb < EqualizerSettings.MAX_GAIN_DB) {
                        onGainChange(
                            (gainDb + 1f).coerceIn(
                                EqualizerSettings.MIN_GAIN_DB,
                                EqualizerSettings.MAX_GAIN_DB
                            )
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(SonoraBg)
                        .border(1.dp, SonoraOutline, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = "Increase $label by 1 dB",
                        tint = if (enabled) SonoraTextSecondary else SonoraTextMuted,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}

fun formatDbValue(value: Float): String {
    val rounded = (value * 10f).roundToInt() / 10f
    return when {
        rounded > 0.05f -> "+%.1f".format(rounded)
        rounded < -0.05f -> "%.1f".format(rounded)
        else -> "0.0"
    }
}
