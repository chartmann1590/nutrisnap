package com.charles.nutrisnap.feature.piproom

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.ChatRepository
import com.charles.nutrisnap.data.UserPreferencesRepository
import com.charles.nutrisnap.data.badge.BadgeRepository
import com.charles.nutrisnap.data.db.ChatMessageEntity
import com.charles.nutrisnap.data.db.ChatRole
import com.charles.nutrisnap.ui.components.PipAccessory
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class PipRoomViewModel @Inject constructor(
    private val badgeRepository: BadgeRepository,
    private val chatRepository: ChatRepository,
    private val userPrefsRepository: UserPreferencesRepository,
) : ViewModel() {

    val currentAccessory: StateFlow<PipAccessory> = userPrefsRepository.pipAccessory
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), PipAccessory.NONE)

    val earnedAccessories: StateFlow<List<PipAccessory>> = badgeRepository.getAll()
        .map { badges ->
            val earnedBadgeNames = badges.map { it.badgeType }.toSet()
            PipAccessory.values().filter { acc ->
                acc == PipAccessory.NONE || acc.unlockedBy?.name in earnedBadgeNames
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), listOf(PipAccessory.NONE))

    val pipLevel: StateFlow<Int> = badgeRepository.getAll()
        .map { (it.size / 5).coerceIn(0, 5) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val pipTitle: StateFlow<String> = pipLevel
        .map { level ->
            listOf("Snack Pal", "Food Friend", "Sous Chef", "Head Chef", "Nutrition Ninja", "Pip Master")[level]
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Snack Pal")

    val recentPipMessages: StateFlow<List<ChatMessageEntity>> = chatRepository.observeHistory()
        .map { msgs -> msgs.filter { it.role == ChatRole.PIP }.take(3) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun equipAccessory(accessory: PipAccessory) {
        viewModelScope.launch { userPrefsRepository.setPipAccessory(accessory) }
    }
}
