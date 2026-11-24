package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Immutable
data class MomoExtraColors(
    val cardBorder: Color,
    val elevatedSurface: Color,
    val mutedText: Color,
    val coralGlow: Color,
    val success: Color,
    val warning: Color,
    val info: Color,
    val isDark: Boolean
)

val LocalMomoExtraColors = staticCompositionLocalOf {
    MomoExtraColors(
        cardBorder = CharcoalBorder,
        elevatedSurface = CharcoalElevated,
        mutedText = CharcoalTextMuted,
        coralGlow = CoralSoftGlow,
        success = StatusSuccessDark,
        warning = StatusWarningDark,
        info = StatusInfoDark,
        isDark = true
    )
}

private val MomoDarkColorScheme = darkColorScheme(
    primary = CoralPrimary,
    onPrimary = CoralOnPrimaryDark,
    primaryContainer = Color(0xFF3A2018),
    onPrimaryContainer = Color(0xFFFFD9CC),
    secondary = Color(0xFF8AA4FF),
    onSecondary = Color(0xFF0F1833),
    secondaryContainer = Color(0xFF1D243B),
    onSecondaryContainer = Color(0xFFD5E0FF),
    tertiary = Color(0xFFF3B57E),
    onTertiary = Color(0xFF2B1807),
    background = CharcoalBg,
    onBackground = CharcoalTextPrimary,
    surface = CharcoalSurface,
    onSurface = CharcoalTextPrimary,
    surfaceVariant = CharcoalSurfaceVariant,
    onSurfaceVariant = CharcoalTextSecondary,
    surfaceContainerLowest = CharcoalBg,
    surfaceContainerLow = CharcoalSurface,
    surfaceContainer = CharcoalSurface,
    surfaceContainerHigh = CharcoalSurfaceVariant,
    surfaceContainerHighest = CharcoalElevated,
    outline = CharcoalBorder,
    outlineVariant = Color(0xFF1F2331),
    error = StatusErrorDark,
    onError = Color(0xFF2C0808),
    errorContainer = Color(0xFF3E1517),
    onErrorContainer = Color(0xFFFFDAD6)
)

private val MomoLightColorScheme = lightColorScheme(
    primary = CoralPrimaryDark,
    onPrimary = CoralOnPrimaryLight,
    primaryContainer = Color(0xFFFFE5DB),
    onPrimaryContainer = Color(0xFF54200E),
    secondary = Color(0xFF445E91),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE5ECFA),
    onSecondaryContainer = Color(0xFF162749),
    tertiary = Color(0xFF9C5E36),
    onTertiary = Color(0xFFFFFFFF),
    background = PorcelainBg,
    onBackground = PorcelainTextPrimary,
    surface = PorcelainSurface,
    onSurface = PorcelainTextPrimary,
    surfaceVariant = PorcelainSurfaceVariant,
    onSurfaceVariant = PorcelainTextSecondary,
    surfaceContainerLowest = PorcelainSurface,
    surfaceContainerLow = PorcelainBg,
    surfaceContainer = PorcelainSurface,
    surfaceContainerHigh = PorcelainSurfaceVariant,
    surfaceContainerHighest = PorcelainElevated,
    outline = PorcelainBorder,
    outlineVariant = Color(0xFFECE8DF),
    error = StatusErrorLight,
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFEE2E2),
    onErrorContainer = Color(0xFF7F1D1D)
)

val MomoShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MomoTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) MomoDarkColorScheme else MomoLightColorScheme
    val extraColors = if (darkTheme) {
        MomoExtraColors(
            cardBorder = CharcoalBorder,
            elevatedSurface = CharcoalElevated,
            mutedText = CharcoalTextMuted,
            coralGlow = CoralSoftGlow,
            success = StatusSuccessDark,
            warning = StatusWarningDark,
            info = StatusInfoDark,
            isDark = true
        )
    } else {
        MomoExtraColors(
            cardBorder = PorcelainBorder,
            elevatedSurface = PorcelainElevated,
            mutedText = PorcelainTextMuted,
            coralGlow = Color(0x26D9724B),
            success = StatusSuccessLight,
            warning = StatusWarningLight,
            info = StatusInfoLight,
            isDark = false
        )
    }

    CompositionLocalProvider(LocalMomoExtraColors provides extraColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = MomoTypography,
            shapes = MomoShapes,
            content = content
        )
    }
}
