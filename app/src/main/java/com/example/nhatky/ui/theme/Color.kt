package com.example.nhatky.ui.theme

import androidx.compose.ui.graphics.Color

// Modern & Clean Palette
val PrimaryModern = Color(0xFF6366F1) // Indigo 500
val SecondaryModern = Color(0xFFF43F5E) // Rose 500

val BackgroundLight = Color(0xFFF8FAFC) // Slate 50
val SurfaceLight = Color(0xFFFFFFFF)
val TextPrimary = Color(0xFF0F172A) // Slate 900

val md_theme_light_primary = PrimaryModern
val md_theme_light_onPrimary = Color.White
val md_theme_light_primaryContainer = Color(0xFFE0E7FF)
val md_theme_light_onPrimaryContainer = Color(0xFF312E81)

val md_theme_light_secondary = SecondaryModern
val md_theme_light_onSecondary = Color.White
val md_theme_light_secondaryContainer = Color(0xFFFFE4E6)
val md_theme_light_onSecondaryContainer = Color(0xFF881337)

val md_theme_light_background = BackgroundLight
val md_theme_light_onBackground = TextPrimary
val md_theme_light_surface = SurfaceLight
val md_theme_light_onSurface = TextPrimary
val md_theme_light_error = Color(0xFFEF4444)

// Material Theme Colors - Dark
val md_theme_dark_primary = Color(0xFF818CF8)
val md_theme_dark_onPrimary = Color(0xFF1E1B4B)
val md_theme_dark_primaryContainer = Color(0xFF6F4325)
val md_theme_dark_onPrimaryContainer = Color(0xFFFFDCC0)
val md_theme_dark_background = Color(0xFF0F172A)
val md_theme_dark_onBackground = Color(0xFFE6E1E5)
val md_theme_dark_surface = Color(0xFF1E293B)
val md_theme_dark_onSurface = Color(0xFFE6E1E5)
val md_theme_dark_error = Color(0xFFF87171)

// Legacy compatibility (re-adding missing refs used in components)
val PaperIvory = Color(0xFFFDFBF5)
val SunsetGradientStart = Color(0xFFFAD0C4)
val SunsetGradientEnd = Color(0xFFFFD1FF)
