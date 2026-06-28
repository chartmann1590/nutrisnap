package com.charles.nutrisnap.data.challenge

import com.charles.nutrisnap.data.badge.BadgeType

enum class DailyChallengeType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val rewardBadge: BadgeType?
) {
    LOG_BEFORE_NOON(
        displayName = "Log Before Noon",
        description = "Log at least one meal before noon",
        emoji = "🕛",
        rewardBadge = BadgeType.EARLY_BIRD
    ),
    HIT_PROTEIN(
        displayName = "Protein Power",
        description = "Hit your daily protein goal",
        emoji = "💪",
        rewardBadge = BadgeType.PROTEIN_CHAMP
    ),
    LOG_THREE_MEALS(
        displayName = "Triple Logger",
        description = "Log breakfast, lunch, and dinner today",
        emoji = "🥗",
        rewardBadge = BadgeType.DAILY_TRIPLE
    ),
    BALANCED_MACROS(
        displayName = "Macro Balance",
        description = "Hit all your macro targets today",
        emoji = "⚖️",
        rewardBadge = BadgeType.BALANCED_DAY
    ),
    EARLY_BREAKFAST(
        displayName = "Rise and Shine",
        description = "Log breakfast before 8 AM",
        emoji = "🌅",
        rewardBadge = BadgeType.MORNING_PERSON
    ),
    HIT_CALORIE_GOAL(
        displayName = "Calorie Goal",
        description = "Finish the day within 100 kcal of your goal",
        emoji = "🎯",
        rewardBadge = BadgeType.CHALLENGE_CHAMP
    ),
    LIGHT_DAY(
        displayName = "Light Day",
        description = "Keep calories under 1200 today",
        emoji = "🥦",
        rewardBadge = BadgeType.LIGHT_EATER
    ),
}
