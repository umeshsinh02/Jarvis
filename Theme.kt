package com.jarvis.assistant.presentation.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import com.jarvis.assistant.domain.model.HudThemeStyle

private val JarvisColorScheme = darkColorScheme(
    primary = JarvisCyan,
    onPrimary = DarkNavy,
    secondary = NeonBlue,
    onSecondary = IceWhite,
    background = DarkNavy,
    onBackground = IceWhite,
    surface = DeepSlateNavy,
    onSurface = IceWhite,
    surfaceVariant = GlassNavy,
    onSurfaceVariant = MutedSteel,
    error = AlertCrimson
)

@Composable
fun JarvisTheme(
    themeStyle: HudThemeStyle = HudThemeStyle.ARC_CYAN,
    content: @Composable () -> Unit
) {
    val accent = when (themeStyle) {
        HudThemeStyle.ARC_CYAN -> JarvisCyan
        HudThemeStyle.STARK_COBALT -> NeonBlue
        HudThemeStyle.STEALTH_ICE -> IceWhite
    }

    MaterialTheme(
        colorScheme = JarvisColorScheme.copy(primary = accent),
        typography = JarvisTypography,
        content = content
    )
}
