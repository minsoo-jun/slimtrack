package com.minsoo.slimtrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val TealPrimary = Color(0xFF0F766E)
val TealSecondary = Color(0xFF115E59)
val TealContainer = Color(0xFFCCFBF1)
val OnTealContainer = Color(0xFF042F2E)

val StatusGreenBg = Color(0xFFDCFCE7)
val StatusGreenBorder = Color(0xFF16A34A)
val StatusGreenText = Color(0xFF14532D)

val StatusYellowBg = Color(0xFFFEF9C3)
val StatusYellowBorder = Color(0xFFCA8A04)
val StatusYellowText = Color(0xFF713F12)

val StatusRedBg = Color(0xFFFEE2E2)
val StatusRedBorder = Color(0xFFDC2626)
val StatusRedText = Color(0xFF7F1D1D)

val StatusGrayBg = Color(0xFFF1F5F9)
val StatusGrayBorder = Color(0xFFCBD5E1)
val StatusGrayText = Color(0xFF475569)

private val LightColors = lightColorScheme(
    primary = TealPrimary,
    onPrimary = Color.White,
    primaryContainer = TealContainer,
    onPrimaryContainer = OnTealContainer,
    secondary = TealSecondary,
    onSecondary = Color.White,
    background = Color(0xFFF8FAFC),
    surface = Color.White,
    surfaceVariant = Color(0xFFF1F5F9),
    error = Color(0xFFB91C1C)
)

@Composable
fun SlimTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        content = content
    )
}
