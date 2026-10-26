package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val GaumeLightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E3A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6EFE0),
    onPrimaryContainer = Color(0xFF092917),
    secondary = ClayTerracotta,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFCE2D7),
    onSecondaryContainer = Color(0xFF3F1607),
    tertiary = SemoisRiverTeal,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFCFF2F5),
    onTertiaryContainer = Color(0xFF00282C),
    background = Color(0xFFF6F8F5),
    onBackground = Color(0xFF142119),
    surface = Color.White,
    onSurface = Color(0xFF142119),
    surfaceVariant = Color(0xFFE5ECE7),
    onSurfaceVariant = Color(0xFF425449),
    outline = Color(0xFF728679),
    outlineVariant = Color(0xFFC7D3CB)
)

private val GaumeDarkColorScheme = darkColorScheme(
    primary = GaumeEmerald,
    onPrimary = GaumeSlateBlack,
    primaryContainer = Color(0xFF123829),
    onPrimaryContainer = GaumeLimestone,
    secondary = OrvalCopperAmber,
    onSecondary = GaumeSlateBlack,
    secondaryContainer = OrvalAmberSoft,
    onSecondaryContainer = Color(0xFFFFDCC2),
    tertiary = RullesHopGold,
    onTertiary = GaumeSlateBlack,
    tertiaryContainer = RullesGoldSoft,
    onTertiaryContainer = Color(0xFFFFECB0),
    background = GaumeSlateBlack,
    onBackground = GaumeLimestone,
    surface = GaumeDeepForest,
    onSurface = GaumeLimestone,
    surfaceVariant = GaumeElevatedMoss,
    onSurfaceVariant = GaumeMist,
    surfaceContainer = GaumeMossCard,
    surfaceContainerHigh = GaumeElevatedMoss,
    outline = GaumePineBorder,
    outlineVariant = Color(0xFF1E3328)
)

@Composable
fun GaumeTrailAlgTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) GaumeDarkColorScheme else GaumeLightColorScheme,
        typography = Typography,
        content = content
    )
}

