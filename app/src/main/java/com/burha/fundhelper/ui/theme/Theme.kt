package com.burha.fundhelper.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainer,
    onPrimaryContainer = TealOnPrimaryContainer,
    inversePrimary = TealInversePrimaryLight,
    secondary = TealSecondaryLight,
    onSecondary = TealOnSecondaryLight,
    secondaryContainer = TealSecondaryContainerLight,
    onSecondaryContainer = TealOnSecondaryContainerLight,
    tertiary = TealTertiaryLight,
    onTertiary = TealOnTertiaryLight,
    tertiaryContainer = TealTertiaryContainerLight,
    onTertiaryContainer = TealOnTertiaryContainerLight,
    background = BackgroundLight,
    surface = SurfaceLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

private val DarkColors = darkColorScheme(
    primary = TealPrimary,
    onPrimary = TealOnPrimary,
    primaryContainer = TealPrimaryContainerDark,
    onPrimaryContainer = TealPrimaryContainer,
    inversePrimary = TealInversePrimaryDark,
    secondary = TealSecondaryDark,
    onSecondary = TealOnSecondaryDark,
    secondaryContainer = TealSecondaryContainerDark,
    onSecondaryContainer = TealOnSecondaryContainerDark,
    tertiary = TealTertiaryDark,
    onTertiary = TealOnTertiaryDark,
    tertiaryContainer = TealTertiaryContainerDark,
    onTertiaryContainer = TealOnTertiaryContainerDark,
    background = BackgroundDark,
    surface = SurfaceDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

@Composable
fun FundHelperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
