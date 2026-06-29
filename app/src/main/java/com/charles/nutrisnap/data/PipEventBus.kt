package com.charles.nutrisnap.data

import com.charles.nutrisnap.data.badge.BadgeType
import com.charles.nutrisnap.data.challenge.DailyChallengeType
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

sealed class PipEvent {
    data class BadgeEarned(val badge: BadgeType) : PipEvent()
    data class ChallengeComplete(val challenge: DailyChallengeType) : PipEvent()
    data class StreakMilestone(val days: Int) : PipEvent()
    object GoalHit : PipEvent()
    object MealLogged : PipEvent()
    object FirstMealOfDay : PipEvent()
}

@Singleton
class PipEventBus @Inject constructor() {
    private val _events = MutableSharedFlow<PipEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<PipEvent> = _events.asSharedFlow()
    suspend fun emit(event: PipEvent) = _events.emit(event)
}
