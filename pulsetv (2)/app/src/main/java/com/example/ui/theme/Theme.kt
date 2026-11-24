package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val CinematicDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = OnElectricCyan,
    primaryContainer = ElectricCyanContainer,
    onPrimaryContainer = OnElectricCyanContainer,
    secondary = LiveCrimson,
    onSecondary = OnLiveCrimson,
    secondaryContainer = LiveCrimsonContainer,
    onSecondaryContainer = TextPrimary,
    tertiary = AmberGlow,
    onTertiary = OnAmberGlow,
    tertiaryContainer = AmberGlowContainer,
    onTertiaryContainer = TextPrimary,
    background = ObsidianBg,
    onBackground = TextPrimary,
    surface = MidnightSurface,
    onSurface = TextPrimary,
    surfaceVariant = MidnightSurfaceHigh,
    onSurfaceVariant = TextSecondary,
    surfaceContainerLowest = ObsidianBg,
    surfaceContainerLow = MidnightSurfaceLow,
    surfaceContainer = MidnightSurfaceContainer,
    surfaceContainerHigh = MidnightSurfaceHigh,
    surfaceContainerHighest = MidnightSurfaceHighest,
    outline = BorderSubtle,
    outlineVariant = MidnightSurfaceHighest,
    error = LiveCrimson,
    onError = OnLiveCrimson,
)

private val PulseTvShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

@Composable
fun PulseTvTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = CinematicDarkColorScheme,
        typography = PulseTvTypography,
        shapes = PulseTvShapes,
        content = content,
    )
}
