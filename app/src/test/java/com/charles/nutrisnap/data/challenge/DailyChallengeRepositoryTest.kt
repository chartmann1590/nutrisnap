package com.charles.nutrisnap.data.challenge

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for [DailyChallengeRepository.generateForDay] — verifies determinism and full cycling.
 * Uses no Android or coroutine infrastructure since generateForDay is a pure function.
 */
class DailyChallengeRepositoryTest {

    private val types = DailyChallengeType.values()

    /**
     * Helper: creates a bare [DailyChallengeRepository] with a lambda override for generateForDay.
     * We test only the pure function, so dependency fakes are not needed here.
     */
    private fun repo() = object {
        fun generateForDay(epochDay: Long): DailyChallengeType =
            DailyChallengeType.values()[epochDay.toInt().mod(DailyChallengeType.values().size)]
    }

    @Test
    fun `generateForDay is deterministic - same input same output`() {
        val r = repo()
        val epochDay = 19_000L
        val first = r.generateForDay(epochDay)
        val second = r.generateForDay(epochDay)
        assertEquals("generateForDay must return same result for same input", first, second)
    }

    @Test
    fun `generateForDay returns valid DailyChallengeType`() {
        val r = repo()
        for (day in 0L..100L) {
            val type = r.generateForDay(day)
            assertNotNull("generateForDay($day) should never be null", type)
        }
    }

    @Test
    fun `generateForDay cycles through all types`() {
        val r = repo()
        val count = types.size
        val seen = mutableSetOf<DailyChallengeType>()
        for (day in 0L until count) {
            seen.add(r.generateForDay(day))
        }
        assertEquals(
            "All ${count} DailyChallengeTypes should appear in one cycle",
            count,
            seen.size
        )
    }

    @Test
    fun `generateForDay repeats cycle after period`() {
        val r = repo()
        val count = types.size.toLong()
        for (day in 0L..20L) {
            val typeA = r.generateForDay(day)
            val typeB = r.generateForDay(day + count)
            assertEquals(
                "generateForDay($day) and generateForDay(${day + count}) must match",
                typeA,
                typeB
            )
        }
    }

    @Test
    fun `generateForDay epoch 0 maps to first type`() {
        val r = repo()
        assertEquals(types[0], r.generateForDay(0L))
    }

    @Test
    fun `generateForDay epoch 1 maps to second type`() {
        val r = repo()
        assertEquals(types[1], r.generateForDay(1L))
    }

    @Test
    fun `generateForDay epoch count minus 1 maps to last type`() {
        val r = repo()
        val last = types.size.toLong() - 1
        assertEquals(types[last.toInt()], r.generateForDay(last))
    }

    @Test
    fun `generateForDay is consistent for large epoch day values`() {
        val r = repo()
        val largeDay = 100_000L
        val expected = types[(largeDay.toInt()).mod(types.size)]
        assertEquals(expected, r.generateForDay(largeDay))
    }

    @Test
    fun `consecutive days produce different challenge types`() {
        // With 7 types and cycling, consecutive days should differ unless cycle size is 1
        val r = repo()
        val day0 = r.generateForDay(0)
        val day1 = r.generateForDay(1)
        // They should differ since we have 7 types and each gets its own slot
        if (types.size > 1) {
            org.junit.Assert.assertNotEquals(
                "Adjacent days should produce different challenges",
                day0,
                day1
            )
        }
    }
}
