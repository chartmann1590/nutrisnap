package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.badge.BadgeType
import com.charles.nutrisnap.ui.components.PipMood

/**
 * Maps a [PipEvent] to the (mood, speech) pair that Pip should display.
 * Pure object — no Android or injection dependencies.
 */
object PipReactionMapper {
    fun map(event: PipEvent): Pair<PipMood, String> = when (event) {
        is PipEvent.BadgeEarned -> when (event.badge) {
            BadgeType.FIRST_BITE -> PipMood.Celebrate to "Your first meal! I'm SO happy right now! 🎉"
            BadgeType.HOT_STREAK -> PipMood.Celebrate to "7 days! You're absolutely AMAZING! 🔥"
            BadgeType.UNSTOPPABLE -> PipMood.Celebrate to "30 days! You're unstoppable! I'm in awe 🏆"
            BadgeType.CENTURY -> PipMood.Celebrate to "100 meals with me! This calls for a party! 💯🎊"
            BadgeType.CHEF_HAT -> PipMood.Celebrate to "Sous Chef unlocked! Here's your hat! 👨‍🍳"
            else -> PipMood.Celebrate to "New badge: ${event.badge.displayName}! ${event.badge.emoji}"
        }
        is PipEvent.ChallengeComplete -> PipMood.Celebrate to "Challenge CRUSHED! You did it! ⭐"
        is PipEvent.StreakMilestone -> PipMood.Celebrate to when (event.days) {
            7 -> "A whole week! I made you a (virtual) smoothie! 🥤"
            14 -> "Two weeks! You're on FIRE! 🔥🔥"
            30 -> "30 days! You're a legend! 🏆✨"
            else -> "${event.days} days! Keep going! 🎉"
        }
        PipEvent.GoalHit -> PipMood.Proud to "Goal hit! You nailed it today! 💪"
        PipEvent.MealLogged -> PipMood.Content to "Logged! Every meal counts! 🍽️"
        PipEvent.FirstMealOfDay -> PipMood.Proud to "First meal of the day! Great start! ☀️"
    }
}
