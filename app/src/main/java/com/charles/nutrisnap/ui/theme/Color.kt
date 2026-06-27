package com.charles.nutrisnap.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * "Snackable" palette — see design/style-tile.html (the design north star).
 * These are the single source of truth for app colors; Material [androidx.compose.material3.ColorScheme]
 * is derived from them in [Theme.kt], and macro colors are exposed via [NutriColors].
 */

// Surfaces
val Cream = Color(0xFFFFF6EC)      // warm background
val CreamCard = Color(0xFFFFFDF9)  // card surface
val RingTrack = Color(0xFFF0E4D6)  // unfilled ring / track

// Text
val Cocoa = Color(0xFF3A2A21)      // primary text
val CocoaSoft = Color(0xFF8A776B)  // muted text

// Brand
val Mango = Color(0xFFFF9F1C)      // primary
val MangoDeep = Color(0xFFF4860A)  // primary pressed / pop shadow
val Berry = Color(0xFFFF5D73)      // accent / secondary
val BerryDeep = Color(0xFFF23F58)

// Macro + semantic
val Mint = Color(0xFF2EC4A6)       // protein / success
val Grape = Color(0xFF7C5CFC)      // carbs
val Butter = Color(0xFFFFD66B)     // fat / highlight
val Sky = Color(0xFF5BC0EB)        // info

// Soft tints for chips/containers
val MangoTint = Color(0xFFFFF0D6)
val MintTint = Color(0xFFE2F7F1)
val SkyTint = Color(0xFFE9F6FC)
val BerryTint = Color(0xFFFFE0E5)
