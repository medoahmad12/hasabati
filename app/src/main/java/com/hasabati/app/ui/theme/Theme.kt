package com.hasabati.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val LightColors = lightColorScheme(
    primary = PurpleAccent,
    onPrimary = SurfaceWhite,
    primaryContainer = PurpleAccentLight,
    secondary = NavySurface,
    background = BackgroundLight,
    surface = SurfaceWhite,
    onBackground = TextPrimaryDark,
    onSurface = TextPrimaryDark,
    error = DangerRed,
    outline = BorderGray
)

val HasabatiTypography = Typography(
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold, fontSize = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 20.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp)
)

@Composable
fun HasabatiTheme(content: @Composable () -> Unit) {
    // نستخدم نفس النسق دائماً (فاتح) للحفاظ على وضوح الأرقام المالية كما طُلب في القسم 28
    MaterialTheme(
        colorScheme = LightColors,
        typography = HasabatiTypography,
        content = content
    )
}
