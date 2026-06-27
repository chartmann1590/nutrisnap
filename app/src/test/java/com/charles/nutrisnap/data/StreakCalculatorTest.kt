package com.charles.nutrisnap.data

import org.junit.Assert.assertEquals
import org.junit.Test

class StreakCalculatorTest {

    @Test
    fun `empty set has no streak`() {
        assertEquals(0, StreakCalculator.currentStreak(emptySet(), 10L))
        assertEquals(0, StreakCalculator.bestStreak(emptySet()))
    }

    @Test
    fun `single day today is streak of 1`() {
        val logged = setOf(100L)
        assertEquals(1, StreakCalculator.currentStreak(logged, 100L))
        assertEquals(1, StreakCalculator.bestStreak(logged))
    }

    @Test
    fun `single day yesterday gives current 1`() {
        val logged = setOf(99L)
        assertEquals(1, StreakCalculator.currentStreak(logged, 100L))
        assertEquals(1, StreakCalculator.bestStreak(logged))
    }

    @Test
    fun `consecutive days ending today`() {
        val logged = setOf(96L, 97L, 98L, 99L, 100L)
        assertEquals(5, StreakCalculator.currentStreak(logged, 100L))
        assertEquals(5, StreakCalculator.bestStreak(logged))
    }

    @Test
    fun `gap resets current streak`() {
        val logged = setOf(95L, 96L, 97L, 99L, 100L)
        assertEquals(2, StreakCalculator.currentStreak(logged, 100L))
        assertEquals(3, StreakCalculator.bestStreak(logged))
    }

    @Test
    fun `best streak captures longest run not at end`() {
        val logged = setOf(90L, 91L, 92L, 93L, 94L, 95L, 98L, 99L, 100L)
        assertEquals(3, StreakCalculator.currentStreak(logged, 100L))
        assertEquals(6, StreakCalculator.bestStreak(logged))
    }

    @Test
    fun `today not logged but yesterday is`() {
        val logged = setOf(99L)
        assertEquals(1, StreakCalculator.currentStreak(logged, 100L))
    }

    @Test
    fun `streak works with today logged`() {
        val logged = setOf(99L, 100L)
        assertEquals(2, StreakCalculator.currentStreak(logged, 100L))
    }

    @Test
    fun `no streak when gap before today`() {
        val logged = setOf(95L, 100L)
        assertEquals(1, StreakCalculator.currentStreak(logged, 100L))
        assertEquals(1, StreakCalculator.bestStreak(logged))
    }

    @Test
    fun `single consecutive day counts as 1 best streak`() {
        val logged = setOf(50L, 100L, 200L)
        assertEquals(0, StreakCalculator.currentStreak(logged, 300L))
        assertEquals(1, StreakCalculator.bestStreak(logged))
    }
}
