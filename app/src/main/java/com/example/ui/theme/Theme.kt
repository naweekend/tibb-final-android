package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.LayoutDirection
import androidx.core.view.WindowCompat

@Immutable
data class TibbExtendedColors(
    val isDark: Boolean,
    val bgTop: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val ink: Color,
    val muted: Color,
    val ctaBg: Color,
    val ctaInk: Color,
    val accent: Color,
    val accentSoft: Color,
    val border: Color,
    val mistTop: Color,
    val mistBottom: Color,
    val mistInk: Color,
    val espressoTop: Color,
    val espressoBottom: Color,
    val glowCenter: Color,
    val glowEdge: Color,
    val dangerRed: Color,
    val dangerSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val urgentYellow: Color,
    val urgentYellowSwatch: Color,
    val urgentYellowSoft: Color,
    val safeGreen: Color,
    val safeGreenSoft: Color,
    val wellnessBlue: Color,
    val wellnessBlueSoft: Color,
)

private val DarkExtended = TibbExtendedColors(
    isDark = true,
    bgTop = ForestBgTop,
    bgBottom = ForestBgBottom,
    surface = ForestSurface,
    surfaceElevated = ForestSurfaceElevated,
    surfaceMuted = ForestSurfaceMuted,
    ink = ForestInk,
    muted = ForestMuted,
    ctaBg = ForestCtaBg,
    ctaInk = ForestCtaInk,
    accent = ForestAccent,
    accentSoft = ForestAccentStrong.copy(alpha = 0.22f),
    border = ForestBorder,
    mistTop = MistDarkTop,
    mistBottom = MistDarkBottom,
    mistInk = Color(0xFF0E231D),
    espressoTop = EspressoTop,
    espressoBottom = EspressoBottom,
    glowCenter = ThoughtGlowCenterDark,
    glowEdge = ThoughtGlowEdgeDark,
    dangerRed = EmergencyRed,
    dangerSoft = EmergencyRedSoftDark,
    amber = WarningAmberLight,
    amberSoft = WarningAmberSoftDark,
    urgentYellow = UrgentYellowLight,
    urgentYellowSwatch = UrgentYellowSwatch,
    urgentYellowSoft = UrgentYellowSoftDark,
    safeGreen = SafeGreen,
    safeGreenSoft = SafeGreenSoftDark,
    wellnessBlue = WellnessBlueLight,
    wellnessBlueSoft = WellnessBlueSoftDark,
)

private val LightExtended = TibbExtendedColors(
    isDark = false,
    bgTop = CreamBgTop,
    bgBottom = CreamBgBottom,
    surface = CreamSurface,
    surfaceElevated = CreamSurface,
    surfaceMuted = CreamSurfaceMuted,
    ink = CreamInk,
    muted = CreamMuted,
    ctaBg = CreamCtaBg,
    ctaInk = CreamCtaInk,
    accent = CreamAccent,
    accentSoft = CreamAccent.copy(alpha = 0.14f),
    border = CreamBorder,
    mistTop = MistLightTop,
    mistBottom = MistLightBottom,
    mistInk = Color(0xFF122620),
    espressoTop = EspressoTop,
    espressoBottom = EspressoBottom,
    glowCenter = ThoughtGlowCenterLight,
    glowEdge = ThoughtGlowEdgeLight,
    dangerRed = EmergencyRed,
    dangerSoft = EmergencyRedSoftLight,
    amber = WarningAmber,
    amberSoft = WarningAmberSoftLight,
    urgentYellow = UrgentYellow,
    urgentYellowSwatch = UrgentYellowSwatch,
    urgentYellowSoft = UrgentYellowSoftLight,
    safeGreen = SafeGreen,
    safeGreenSoft = SafeGreenSoftLight,
    wellnessBlue = WellnessBlue,
    wellnessBlueSoft = WellnessBlueSoftLight,
)

val LocalTibbColors = staticCompositionLocalOf { DarkExtended }

private val DarkM3Scheme = darkColorScheme(
    primary = ForestCtaBg,
    onPrimary = ForestCtaInk,
    secondary = ForestAccent,
    onSecondary = ForestCtaInk,
    tertiary = ForestAccentStrong,
    background = ForestBgTop,
    onBackground = ForestInk,
    surface = ForestSurface,
    onSurface = ForestInk,
    surfaceVariant = ForestSurfaceMuted,
    onSurfaceVariant = ForestMuted,
    outline = ForestBorder,
    error = EmergencyRed,
    onError = Color.White,
)

private val LightM3Scheme = lightColorScheme(
    primary = CreamCtaBg,
    onPrimary = CreamCtaInk,
    secondary = CreamAccent,
    onSecondary = Color.White,
    tertiary = CreamAccent,
    background = CreamBgTop,
    onBackground = CreamInk,
    surface = CreamSurface,
    onSurface = CreamInk,
    surfaceVariant = CreamSurfaceMuted,
    onSurfaceVariant = CreamMuted,
    outline = CreamBorder,
    error = EmergencyRed,
    onError = Color.White,
)

@Composable
fun TibbTheme(
    darkTheme: Boolean = true,
    isUrduRtl: Boolean = false,
    content: @Composable () -> Unit,
) {
    val extended = if (darkTheme) DarkExtended else LightExtended
    val colorScheme = if (darkTheme) DarkM3Scheme else LightM3Scheme
    val layoutDirection = if (isUrduRtl) LayoutDirection.Rtl else LayoutDirection.Ltr

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                @Suppress("DEPRECATION")
                window.statusBarColor = Color.Transparent.toArgb()
                @Suppress("DEPRECATION")
                window.navigationBarColor = Color.Transparent.toArgb()
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !darkTheme
                controller.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(
        LocalTibbColors provides extended,
        LocalLayoutDirection provides layoutDirection,
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = TibbTypography,
            shapes = TibbShapes,
            content = content,
        )
    }
}
