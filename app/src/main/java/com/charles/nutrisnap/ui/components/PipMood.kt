package com.charles.nutrisnap.ui.components

/**
 * The set of moods Pip can express. [pipMoodFor] derives steady-state moods from
 * dashboard data; [Celebrate] and [Thinking] are set explicitly by the UI for
 * transient events (a meal just logged) or specific screens (AI analysis).
 */
enum class PipMood {
    Content,
    Sleepy,
    Celebrate,
    Proud,
    Stuffed,
    Thinking,
}
