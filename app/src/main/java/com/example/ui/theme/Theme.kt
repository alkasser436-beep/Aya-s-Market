package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

private val AyaLightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = Color.White,
    primaryContainer = EmeraldContainer,
    onPrimaryContainer = OnEmeraldContainer,
    secondary = WarmOrangeSecondary,
    onSecondary = Color.White,
    secondaryContainer = WarmOrangeContainer,
    onSecondaryContainer = OnWarmOrangeContainer,
    tertiary = FreshMintTertiary,
    onTertiary = Color.White,
    tertiaryContainer = FreshMintContainer,
    background = CrispBackground,
    onBackground = TextPrimary,
    surface = CrispSurface,
    onSurface = TextPrimary,
    surfaceVariant = SoftSurfaceVariant,
    onSurfaceVariant = TextSecondary,
    outline = OutlineMint,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = ErrorContainer
)

private val AyaDarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkOnEmerald,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = EmeraldContainer,
    secondary = DarkOrangeSecondary,
    onSecondary = OnWarmOrangeContainer,
    secondaryContainer = Color(0xFF5A2E00),
    onSecondaryContainer = WarmOrangeContainer,
    tertiary = FreshMintTertiary,
    background = DarkBackground,
    onBackground = Color(0xFFE6F2EC),
    surface = DarkSurface,
    onSurface = Color(0xFFE6F2EC),
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = Color(0xFFA7C4B5),
    outline = Color(0xFF334F42),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A)
)

val AyaShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AyaMarketTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    isArabic: Boolean = false,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> AyaDarkColorScheme
        else -> AyaLightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ayaTypography(isArabic = isArabic),
        shapes = AyaShapes,
        content = content
    )
}
