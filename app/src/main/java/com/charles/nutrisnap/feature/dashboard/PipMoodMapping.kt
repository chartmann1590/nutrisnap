package com.charles.nutrisnap.feature.dashboard

import com.charles.nutrisnap.ui.components.PipMood

/**
 * Maps a dashboard snapshot to Pip's steady-state mood. Pure — no Compose, no IO.
 * Priority: over-goal (gentle) > nothing logged > active streak > content.
 * Never returns [PipMood.Celebrate] or [PipMood.Thinking]; those are set by the UI.
 */
fun pipMoodFor(state: DashboardUiState): PipMood {
    val remaining = state.remaining ?: return PipMood.Content
    return when {
        remaining.kcalRemaining < 0 -> PipMood.Stuffed
        state.todayMeals.isEmpty() -> PipMood.Sleepy
        state.streak >= 2 -> PipMood.Proud
        else -> PipMood.Content
    }
}
