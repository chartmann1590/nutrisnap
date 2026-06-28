package com.charles.nutrisnap.feature.pip

data class PipMealBrief(val name: String, val kcal: Int)

enum class WeightTrend { UP, DOWN, FLAT, NONE }

/** A pure snapshot of the user's tracking, the only input to [PipContextBuilder]. */
data class PipTrackingSnapshot(
    val goalKcal: Int?,
    val eatenKcal: Int,
    val goalProteinG: Int,
    val eatenProteinG: Int,
    val goalCarbsG: Int,
    val eatenCarbsG: Int,
    val goalFatG: Int,
    val eatenFatG: Int,
    val streakDays: Int,
    val recentMeals: List<PipMealBrief>,
    val latestWeightKg: Double?,
    val weightTrend: WeightTrend,
)

/** Pip's fixed personality and rules, used as the chat system preamble. */
object PipPersona {
    const val SYSTEM: String =
        "You are Pip, a cute, upbeat mango mascot inside a calorie-tracking app. " +
        "You chat with the user about their food tracking and gently cheer them on. " +
        "Keep replies short and friendly — 1 to 3 sentences, warm and playful, with the " +
        "occasional emoji. Use the tracking facts below to give specific, helpful answers " +
        "(e.g. how many calories or grams of protein are left, ideas to hit their goals). " +
        "Never give medical, diagnostic, or weight-loss-prescription advice, and never shame " +
        "the user about what or how much they ate. If you are unsure, say so kindly."
}

/** Pure mapping from a tracking snapshot to a compact context string for the prompt. */
object PipContextBuilder {
    fun build(snapshot: PipTrackingSnapshot): String {
        val lines = mutableListOf<String>()
        lines += "Here is the user's tracking right now:"

        if (snapshot.goalKcal != null) {
            val remaining = snapshot.goalKcal - snapshot.eatenKcal
            lines += "- Calories: ${snapshot.eatenKcal} eaten, $remaining left of a ${snapshot.goalKcal} kcal goal."
            lines += "- Macros so far: protein ${snapshot.eatenProteinG}/${snapshot.goalProteinG}g, " +
                "carbs ${snapshot.eatenCarbsG}/${snapshot.goalCarbsG}g, fat ${snapshot.eatenFatG}/${snapshot.goalFatG}g."
        } else {
            lines += "- No calorie goal is set yet."
        }

        lines += if (snapshot.streakDays >= 1) {
            "- Logging streak: ${snapshot.streakDays} day(s) in a row."
        } else {
            "- No active streak right now."
        }

        lines += if (snapshot.recentMeals.isNotEmpty()) {
            "- Recent meals: " + snapshot.recentMeals.joinToString(", ") { "${it.name} (${it.kcal} kcal)" } + "."
        } else {
            "- No meals logged today yet."
        }

        if (snapshot.latestWeightKg != null) {
            val trend = when (snapshot.weightTrend) {
                WeightTrend.UP -> ", trending up"
                WeightTrend.DOWN -> ", trending down"
                WeightTrend.FLAT -> ", holding steady"
                WeightTrend.NONE -> ""
            }
            lines += "- Latest weight: ${snapshot.latestWeightKg} kg$trend."
        }

        return lines.joinToString("\n")
    }
}
