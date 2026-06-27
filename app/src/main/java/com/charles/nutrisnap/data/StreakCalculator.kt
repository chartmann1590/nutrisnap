package com.charles.nutrisnap.data

object StreakCalculator {

    fun currentStreak(loggedDays: Set<Long>, todayEpochDay: Long): Int {
        val checkDay = if (todayEpochDay in loggedDays) todayEpochDay else todayEpochDay - 1
        if (checkDay !in loggedDays) return 0
        var streak = 0
        var day = checkDay
        while (day in loggedDays) {
            streak++
            day--
        }
        return streak
    }

    fun bestStreak(loggedDays: Set<Long>): Int {
        if (loggedDays.isEmpty()) return 0
        val sorted = loggedDays.sorted()
        var best = 1
        var current = 1
        for (i in 1 until sorted.size) {
            if (sorted[i] == sorted[i - 1] + 1) {
                current++
                if (current > best) best = current
            } else {
                current = 1
            }
        }
        return best
    }
}
