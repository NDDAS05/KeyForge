package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val KeyForgeDarkColorScheme = darkColorScheme(
    primary = ElectricCyan,
    onPrimary = Color(0xFF002730),
    primaryContainer = ElectricCyanDim,
    onPrimaryContainer = Color(0xFFB5F5FF),
    secondary = AmberSyntax,
    onSecondary = Color(0xFF2B1800),
    secondaryContainer = AmberSyntaxDim,
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary = EmeraldPair,
    onTertiary = Color(0xFF00291B),
    tertiaryContainer = EmeraldPairDim,
    onTertiaryContainer = Color(0xFFA7F3D0),
    background = ObsidianBg,
    onBackground = InkLight,
    surface = ObsidianSurface,
    onSurface = InkLight,
    surfaceVariant = ObsidianSurfaceHigh,
    onSurfaceVariant = InkMuted,
    surfaceContainerLowest = Color(0xFF070B10),
    surfaceContainerLow = ObsidianSurface,
    surfaceContainer = ObsidianSurfaceHigh,
    surfaceContainerHigh = ObsidianSurfaceHighest,
    surfaceContainerHighest = Color(0xFF2B394E),
    outline = ObsidianBorder,
    outlineVariant = Color(0xFF1B2636),
    error = RoseAlert,
    onError = Color.White
)

private val KeyForgeLightColorScheme = lightColorScheme(
    primary = CobaltPrimary,
    onPrimary = Color.White,
    primaryContainer = CobaltContainer,
    onPrimaryContainer = Color(0xFF00344F),
    secondary = OchreSecondary,
    onSecondary = Color.White,
    secondaryContainer = OchreContainer,
    onSecondaryContainer = Color(0xFF451A03),
    tertiary = MintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = MintContainer,
    onTertiaryContainer = Color(0xFF022C22),
    background = PaperBg,
    onBackground = InkDark,
    surface = PaperSurface,
    onSurface = InkDark,
    surfaceVariant = PaperSurfaceHigh,
    onSurfaceVariant = InkDarkMuted,
    surfaceContainerLowest = Color(0xFFEFF3F8),
    surfaceContainerLow = PaperSurface,
    surfaceContainer = PaperSurfaceHigh,
    surfaceContainerHigh = PaperSurfaceHighest,
    surfaceContainerHighest = Color(0xFFCFD9E6),
    outline = PaperBorder,
    outlineVariant = Color(0xFFE2E8F0),
    error = RoseAlert,
    onError = Color.White
)

@Composable
fun KeyForgeTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) KeyForgeDarkColorScheme else KeyForgeLightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = KeyForgeTypography,
        content = content
    )
}
