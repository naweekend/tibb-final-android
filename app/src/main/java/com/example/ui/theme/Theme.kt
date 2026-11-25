package com.example.ui.theme

import android.app.Activity
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
    val bgMid: Color,
    val bgBottom: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceMuted: Color,
    val lilacGrey: Color,
    val ink: Color,
    val muted: Color,
    val ctaBg: Color,
    val ctaInk: Color,
    val secondaryPillBg: Color,
    val secondaryPillInk: Color,
    val accent: Color,
    val accentPastel: Color,
    val accentSoft: Color,
    val border: Color,
    val mistTop: Color,
    val mistBottom: Color,
    val mistInk: Color,
    val espressoTop: Color,
    val espressoBottom: Color,
    val espressoInk: Color,
    val espressoMuted: Color,
    val espressoAccent: Color,
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

private val LightExtended = TibbExtendedColors(
    isDark = false,
    bgTop = IvoryBgTop,
    bgMid = IvoryBgMid,
    bgBottom = IvoryBgBottom,
    surface = SoftSurface,
    surfaceElevated = SoftSurfaceElevated,
    surfaceMuted = SoftSurfaceMuted,
    lilacGrey = MutedLilacGrey,
    ink = SoftCharcoal,
    muted = SoftMutedInk,
    ctaBg = SoftCharcoal,
    ctaInk = SoftCharcoalInk,
    secondaryPillBg = LightLavender,
    secondaryPillInk = SoftCharcoal,
    accent = PastelLavenderDeep,
    accentPastel = PastelLavender,
    accentSoft = LightLavender,
    border = SoftHairlineBorder,
    mistTop = MistLightTop,
    mistBottom = MistLightBottom,
    mistInk = SoftCharcoal,
    espressoTop = EspressoTop,
    espressoBottom = EspressoBottom,
    espressoInk = IvoryBgTop,
    espressoMuted = DarkMuted,
    espressoAccent = PastelLavender,
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

private val DarkExtended = TibbExtendedColors(
    isDark = true,
    bgTop = DarkBgTop,
    bgMid = DarkBgTop,
    bgBottom = DarkBgBottom,
    surface = DarkSurface,
    surfaceElevated = DarkSurfaceElevated,
    surfaceMuted = DarkSurfaceMuted,
    lilacGrey = Color(0xFF423E4E),
    ink = DarkInk,
    muted = DarkMuted,
    ctaBg = DarkCtaBg,
    ctaInk = DarkCtaInk,
    secondaryPillBg = DarkSurfaceMuted,
    secondaryPillInk = DarkInk,
    accent = DarkAccent,
    accentPastel = PastelLavender,
    accentSoft = DarkAccentSoft,
    border = DarkBorder,
    mistTop = MistDarkTop,
    mistBottom = MistDarkBottom,
    mistInk = DarkInk,
    espressoTop = DarkSurfaceElevated,
    espressoBottom = Color(0xFF1A191F),
    espressoInk = DarkInk,
    espressoMuted = DarkMuted,
    espressoAccent = PastelLavender,
    glowCenter = ThoughtGlowCenterDark,
    glowEdge = ThoughtGlowEdgeDark,
    dangerRed = EmergencyRedDark,
    dangerSoft = EmergencyRedSoftDark,
    amber = WarningAmberLight,
    amberSoft = WarningAmberSoftDark,
    urgentYellow = UrgentYellowLight,
    urgentYellowSwatch = UrgentYellowSwatch,
    urgentYellowSoft = UrgentYellowSoftDark,
    safeGreen = SafeGreenLight,
    safeGreenSoft = SafeGreenSoftDark,
    wellnessBlue = WellnessBlueLight,
    wellnessBlueSoft = WellnessBlueSoftDark,
)

val LocalTibbColors = staticCompositionLocalOf { LightExtended }

private val LightM3Scheme = lightColorScheme(
    primary = SoftCharcoal,
    onPrimary = SoftCharcoalInk,
    primaryContainer = LightLavender,
    onPrimaryContainer = SoftCharcoal,
    secondary = PastelLavenderDeep,
    onSecondary = Color.White,
    secondaryContainer = SoftSurfaceMuted,
    onSecondaryContainer = SoftCharcoal,
    tertiary = PastelLavender,
    background = IvoryBgTop,
    onBackground = SoftCharcoal,
    surface = SoftSurfaceElevated,
    onSurface = SoftCharcoal,
    surfaceVariant = SoftSurfaceMuted,
    onSurfaceVariant = SoftMutedInk,
    outline = Color(0x40242326),
    outlineVariant = SoftHairlineBorder,
    error = EmergencyRed,
    onError = Color.White,
)

private val DarkM3Scheme = darkColorScheme(
    primary = DarkCtaBg,
    onPrimary = DarkCtaInk,
    primaryContainer = DarkSurfaceMuted,
    onPrimaryContainer = DarkInk,
    secondary = DarkAccent,
    onSecondary = DarkCtaInk,
    secondaryContainer = DarkSurfaceElevated,
    onSecondaryContainer = DarkInk,
    tertiary = PastelLavender,
    background = DarkBgTop,
    onBackground = DarkInk,
    surface = DarkSurface,
    onSurface = DarkInk,
    surfaceVariant = DarkSurfaceMuted,
    onSurfaceVariant = DarkMuted,
    outline = Color(0x44F5EEDB),
    outlineVariant = DarkBorder,
    error = EmergencyRedDark,
    onError = Color(0xFF2D1215),
)

@Composable
fun TibbTheme(
    darkTheme: Boolean = false,
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
