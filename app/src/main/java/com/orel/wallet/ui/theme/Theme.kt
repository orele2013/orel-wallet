package com.orel.wallet.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.orel.wallet.domain.ThemeMode
import com.orel.wallet.domain.WalletSettings

val Ink = Color(0xFF111C30)
val Muted = Color(0xFF5F6B82)
val Blue = Color(0xFF0866F5)

@Composable
fun OrelTheme(settings: WalletSettings, content: @Composable () -> Unit) {
    val dark = when (settings.themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
    }
    val accent = Color(settings.accentColor)
    val scheme = if (dark) darkColorScheme(
        primary = accent, onPrimary = Color.White,
        background = Color(0xFF090F1A), onBackground = Color(0xFFF4F7FF),
        surface = Color(0xFF111B2B), onSurface = Color(0xFFF4F7FF),
        surfaceVariant = Color(0xFF192438), onSurfaceVariant = Color(0xFF9CAAC1),
        outlineVariant = Color(0xFF26334A), secondaryContainer = Color(0xFF142F57)
    ) else lightColorScheme(
        primary = accent, onPrimary = Color.White,
        background = Color(0xFFF6F8FC), onBackground = Ink,
        surface = Color.White, onSurface = Ink,
        surfaceVariant = Color(0xFFEDF2FA), onSurfaceVariant = Muted,
        outlineVariant = Color(0xFFE4EAF4), secondaryContainer = Color(0xFFE9F1FF)
    )
    MaterialTheme(colorScheme = scheme, typography = Typography(
        headlineLarge = TextStyle(fontFamily = FontFamily.SansSerif, fontWeight = FontWeight.SemiBold, fontSize = 34.sp, letterSpacing = (-1.2).sp),
        headlineMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 28.sp, letterSpacing = (-0.8).sp),
        titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 22.sp, letterSpacing = (-0.5).sp),
        titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
        bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
        labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp),
        labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 10.sp, letterSpacing = 1.sp)
    ), content = content)
}
