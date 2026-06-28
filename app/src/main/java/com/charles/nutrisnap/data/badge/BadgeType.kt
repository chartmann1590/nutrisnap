package com.charles.nutrisnap.data.badge

enum class BadgeType(
    val displayName: String,
    val description: String,
    val emoji: String,
    val colorHex: String
) {
    // Meal count milestones
    FIRST_BITE(
        displayName = "First Bite",
        description = "Log your first meal",
        emoji = "🍽️",
        colorHex = "#FF5D73"
    ),
    SEASONED(
        displayName = "Seasoned",
        description = "Log 10 meals",
        emoji = "🧂",
        colorHex = "#FF9F1C"
    ),
    HALF_CENTURY(
        displayName = "Half Century",
        description = "Log 50 meals",
        emoji = "🥈",
        colorHex = "#A8DADC"
    ),
    CHEF_HAT(
        displayName = "Chef Hat",
        description = "Log 25 meals",
        emoji = "👨‍🍳",
        colorHex = "#FF9F1C"
    ),
    CENTURY(
        displayName = "Century",
        description = "Log 100 meals",
        emoji = "💯",
        colorHex = "#FFD66B"
    ),

    // Streaks
    ON_A_ROLL(
        displayName = "On a Roll",
        description = "Log meals 3 days in a row",
        emoji = "🔥",
        colorHex = "#FF5D73"
    ),
    HOT_STREAK(
        displayName = "Hot Streak",
        description = "Log meals 7 days in a row",
        emoji = "🌶️",
        colorHex = "#FF5D73"
    ),
    FORTNIGHT(
        displayName = "Fortnight",
        description = "Log meals 14 days in a row",
        emoji = "📅",
        colorHex = "#FF9F1C"
    ),
    MONTHLY(
        displayName = "Monthly",
        description = "Log meals 30 days in a row",
        emoji = "🗓️",
        colorHex = "#FFD66B"
    ),
    UNSTOPPABLE(
        displayName = "Unstoppable",
        description = "Log meals 30 days in a row without missing a day",
        emoji = "⚡",
        colorHex = "#FFD66B"
    ),

    // Time-based
    EARLY_BIRD(
        displayName = "Early Bird",
        description = "Log breakfast before 9 AM",
        emoji = "🐦",
        colorHex = "#2EC4A6"
    ),
    NIGHT_OWL(
        displayName = "Night Owl",
        description = "Log dinner after 8 PM",
        emoji = "🦉",
        colorHex = "#457B9D"
    ),
    MORNING_PERSON(
        displayName = "Morning Person",
        description = "Log breakfast before 8 AM seven times",
        emoji = "🌅",
        colorHex = "#FF9F1C"
    ),
    STREAK_SAVER(
        displayName = "Streak Saver",
        description = "Log a meal after midnight to save your streak",
        emoji = "🌙",
        colorHex = "#457B9D"
    ),

    // Meal variety
    DAILY_TRIPLE(
        displayName = "Daily Triple",
        description = "Log breakfast, lunch, and dinner in one day",
        emoji = "🥗",
        colorHex = "#2EC4A6"
    ),
    THREE_PEAT(
        displayName = "Three-Peat",
        description = "Hit your calorie goal 3 days in a row",
        emoji = "🏆",
        colorHex = "#FFD66B"
    ),
    VARIETY(
        displayName = "Variety",
        description = "Log 3 different food names in one day",
        emoji = "🌈",
        colorHex = "#2EC4A6"
    ),

    // Nutrition
    BALANCED_DAY(
        displayName = "Balanced Day",
        description = "Hit all macro goals in a single day",
        emoji = "⚖️",
        colorHex = "#2EC4A6"
    ),
    PROTEIN_CHAMP(
        displayName = "Protein Champ",
        description = "Hit your protein goal 3 times",
        emoji = "💪",
        colorHex = "#FF5D73"
    ),
    LIGHT_EATER(
        displayName = "Light Eater",
        description = "Stay under 1200 kcal in a day",
        emoji = "🥦",
        colorHex = "#2EC4A6"
    ),

    // Challenges & goals
    CHALLENGE_CHAMP(
        displayName = "Challenge Champ",
        description = "Complete a daily challenge",
        emoji = "🎯",
        colorHex = "#FF9F1C"
    ),
    CONSISTENT(
        displayName = "Consistent",
        description = "Hit your calorie goal 5 days in one week",
        emoji = "📊",
        colorHex = "#2EC4A6"
    ),

    // Calendar
    FIRST_WEEK(
        displayName = "First Week",
        description = "Log meals for 7 days total",
        emoji = "📆",
        colorHex = "#A8DADC"
    ),
    MONDAY_CHAMP(
        displayName = "Monday Champ",
        description = "Log meals every Monday for a month",
        emoji = "💼",
        colorHex = "#457B9D"
    ),
    WEEKEND_WARRIOR(
        displayName = "Weekend Warrior",
        description = "Log meals on both Saturday and Sunday",
        emoji = "🏋️",
        colorHex = "#FF9F1C"
    ),
}
