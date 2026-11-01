package com.leaftodo.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

private val LeafColors = lightColorScheme(
    primary = clay, onPrimary = ink, primaryContainer = clayTint, onPrimaryContainer = ink,
    secondary = soft, onSecondary = card, secondaryContainer = clayTint, onSecondaryContainer = ink,
    tertiary = clayText, onTertiary = card, tertiaryContainer = clayTint, onTertiaryContainer = ink,
    background = bg, onBackground = ink, surface = bg, onSurface = ink,
    surfaceVariant = card, onSurfaceVariant = soft, surfaceTint = bg,
    surfaceBright = bg, surfaceDim = bg,
    surfaceContainerLowest = card, surfaceContainerLow = card, surfaceContainer = card,
    surfaceContainerHigh = card, surfaceContainerHighest = card,
    outline = soft, outlineVariant = rule, inverseSurface = ink, inverseOnSurface = card,
    inversePrimary = clayTint, error = clayText, onError = card,
    errorContainer = clayTint, onErrorContainer = ink, scrim = ink,
)

// Always use the fixed light palette; system dark mode and dynamic colors do not apply.
@Composable
fun LeafTheme(content: @Composable () -> Unit) {
    val context = LocalContext.current
    val typography = remember(context) { leafTypography(context) }
    MaterialTheme(colorScheme = LeafColors, typography = typography, content = content)
}
