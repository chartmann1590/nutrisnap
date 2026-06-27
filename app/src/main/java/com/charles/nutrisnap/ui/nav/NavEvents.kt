package com.charles.nutrisnap.ui.nav

sealed interface NavEvent {
    data object Back : NavEvent
    data object ToHome : NavEvent
    data class ToScanResult(val estimateKey: String) : NavEvent
    data class ToEntry(val mode: String, val bitmapKey: String? = null) : NavEvent
    data class ToEditMeal(val mealId: Long) : NavEvent
}