package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val SonoraDarkColorScheme = darkColorScheme(
    primary = SonoraEmerald,
    onPrimary = SonoraBg,
    primaryContainer = SonoraEmeraldDim,
    onPrimaryContainer = SonoraTextPrimary,
    secondary = SonoraTeal,
    onSecondary = SonoraBg,
    secondaryContainer = SonoraSurfaceHigh,
    onSecondaryContainer = SonoraTeal,
    tertiary = SonoraAmber,
    onTertiary = SonoraBg,
    tertiaryContainer = SonoraSurfaceElevated,
    onTertiaryContainer = SonoraAmber,
    background = SonoraBg,
    onBackground = SonoraTextPrimary,
    surface = SonoraSurface,
    onSurface = SonoraTextPrimary,
    surfaceVariant = SonoraSurfaceElevated,
    onSurfaceVariant = SonoraTextSecondary,
    surfaceContainerLowest = SonoraBg,
    surfaceContainerLow = SonoraSurface,
    surfaceContainer = SonoraSurfaceElevated,
    surfaceContainerHigh = SonoraSurfaceHigh,
    outline = SonoraOutline,
    outlineVariant = SonoraSurfaceHigh,
    error = SonoraCoral,
    onError = SonoraBg
)

val SonoraShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun SonoraTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SonoraDarkColorScheme,
        typography = SonoraTypography,
        shapes = SonoraShapes,
        content = content
    )
}
