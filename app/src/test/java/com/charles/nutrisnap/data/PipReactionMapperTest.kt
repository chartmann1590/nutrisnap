package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.badge.BadgeType
import com.charles.nutrisnap.data.challenge.DailyChallengeType
import com.charles.nutrisnap.ui.components.PipMood
import org.junit.Assert.assertEquals
import org.junit.Test

class PipReactionMapperTest {

    @Test
    fun `FIRST_BITE maps to Celebrate with correct speech`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.FIRST_BITE))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("Your first meal! I'm SO happy right now! 🎉", text)
    }

    @Test
    fun `HOT_STREAK maps to Celebrate with correct speech`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.HOT_STREAK))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("7 days! You're absolutely AMAZING! 🔥", text)
    }

    @Test
    fun `UNSTOPPABLE maps to Celebrate with correct speech`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.UNSTOPPABLE))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("30 days! You're unstoppable! I'm in awe 🏆", text)
    }

    @Test
    fun `CENTURY maps to Celebrate with correct speech`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.CENTURY))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("100 meals with me! This calls for a party! 💯🎊", text)
    }

    @Test
    fun `CHEF_HAT maps to Celebrate with correct speech`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.CHEF_HAT))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("Sous Chef unlocked! Here's your hat! 👨‍🍳", text)
    }

    @Test
    fun `other badge uses displayName and emoji fallback`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.SEASONED))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("New badge: Seasoned! 🧂", text)
    }

    @Test
    fun `EARLY_BIRD uses displayName and emoji fallback`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.BadgeEarned(BadgeType.EARLY_BIRD))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("New badge: Early Bird! 🐦", text)
    }

    @Test
    fun `ChallengeComplete maps to Celebrate`() {
        val (mood, text) = PipReactionMapper.map(
            PipEvent.ChallengeComplete(DailyChallengeType.HIT_PROTEIN)
        )
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("Challenge CRUSHED! You did it! ⭐", text)
    }

    @Test
    fun `StreakMilestone 7 maps to smoothie message`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.StreakMilestone(7))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("A whole week! I made you a (virtual) smoothie! 🥤", text)
    }

    @Test
    fun `StreakMilestone 14 maps to fire message`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.StreakMilestone(14))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("Two weeks! You're on FIRE! 🔥🔥", text)
    }

    @Test
    fun `StreakMilestone 30 maps to legend message`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.StreakMilestone(30))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("30 days! You're a legend! 🏆✨", text)
    }

    @Test
    fun `StreakMilestone other uses generic keep going message`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.StreakMilestone(5))
        assertEquals(PipMood.Celebrate, mood)
        assertEquals("5 days! Keep going! 🎉", text)
    }

    @Test
    fun `GoalHit maps to Proud`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.GoalHit)
        assertEquals(PipMood.Proud, mood)
        assertEquals("Goal hit! You nailed it today! 💪", text)
    }

    @Test
    fun `MealLogged maps to Content`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.MealLogged)
        assertEquals(PipMood.Content, mood)
        assertEquals("Logged! Every meal counts! 🍽️", text)
    }

    @Test
    fun `FirstMealOfDay maps to Proud`() {
        val (mood, text) = PipReactionMapper.map(PipEvent.FirstMealOfDay)
        assertEquals(PipMood.Proud, mood)
        assertEquals("First meal of the day! Great start! ☀️", text)
    }
}
