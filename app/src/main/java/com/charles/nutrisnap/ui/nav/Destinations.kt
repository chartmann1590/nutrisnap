package com.charles.nutrisnap.ui.nav

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Person
import androidx.compose.ui.graphics.vector.ImageVector

object Routes {
    const val ONBOARDING = "onboarding"
    const val DOWNLOAD = "download"

    const val HOME = "home"
    const val DIARY = "diary"
    const val TRENDS = "trends"
    const val PROFILE = "profile"

    const val SCAN = "scan"
    const val SCAN_RESULT = "scan_result/{estimateKey}"
    const val ENTRY = "entry/{mode}?bitmapKey={bitmapKey}"
    const val EDIT_MEAL = "edit_meal/{mealId}"
    const val SETTINGS = "settings"
    const val PIP_CHAT = "pip_chat"

    fun scanResult(estimateKey: String) = "scan_result/$estimateKey"
    fun entry(mode: String, bitmapKey: String? = null) =
        if (bitmapKey != null) "entry/$mode?bitmapKey=$bitmapKey" else "entry/$mode"
    fun editMeal(mealId: Long) = "edit_meal/$mealId"
}

enum class TopLevelTab(val route: String, val label: String, val icon: ImageVector) {
    HOME(Routes.HOME, "Home", Icons.Rounded.Home),
    DIARY(Routes.DIARY, "Diary", Icons.Rounded.MenuBook),
    TRENDS(Routes.TRENDS, "Trends", Icons.Rounded.BarChart),
    PROFILE(Routes.PROFILE, "Me", Icons.Rounded.Person),
}
