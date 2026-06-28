package com.charles.nutrisnap.feature.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.badge.BadgeRepository
import com.charles.nutrisnap.data.badge.BadgeType
import com.charles.nutrisnap.data.db.BadgeEntity
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class BadgeDisplayItem(
    val type: BadgeType,
    val earnedAtMs: Long?,
    val isNew: Boolean = false,
)

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository,
) : ViewModel() {

    val badges: StateFlow<List<BadgeDisplayItem>> = badgeRepository.getAll()
        .map { earned -> buildDisplayList(earned) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private fun buildDisplayList(earned: List<BadgeEntity>): List<BadgeDisplayItem> {
        val earnedMap = earned.mapNotNull { entity ->
            runCatching { BadgeType.valueOf(entity.badgeType) }.getOrNull()?.let { it to entity }
        }.toMap()
        val earnedItems = earnedMap.entries
            .sortedByDescending { it.value.earnedAtMs }
            .map { BadgeDisplayItem(it.key, it.value.earnedAtMs, !it.value.seen) }
        val lockedItems = BadgeType.values()
            .filter { it !in earnedMap }
            .sortedBy { it.displayName }
            .map { BadgeDisplayItem(it, null) }
        return earnedItems + lockedItems
    }
}
