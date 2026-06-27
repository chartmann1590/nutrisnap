package com.charles.nutrisnap.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.charles.nutrisnap.R

val DisplayFontFamily = FontFamily(Font(R.font.fredoka_family))
val BodyFontFamily = FontFamily(Font(R.font.nunito_family))

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 44.sp, lineHeight = 48.sp, letterSpacing = (-0.5).sp,
    ),
    displayMedium = TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 34.sp, lineHeight = 38.sp, letterSpacing = (-0.25).sp,
    ),
    headlineLarge = TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 28.sp, lineHeight = 34.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 23.sp, lineHeight = 28.sp,
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp, lineHeight = 24.sp,
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 20.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp, lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily, fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp, lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily, fontWeight = FontWeight.Bold,
        fontSize = 14.sp, lineHeight = 18.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 1.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily, fontWeight = FontWeight.ExtraBold,
        fontSize = 10.sp, lineHeight = 13.sp, letterSpacing = 1.sp,
    ),
)
