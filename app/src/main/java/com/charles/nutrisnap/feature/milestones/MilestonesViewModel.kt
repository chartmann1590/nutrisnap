package com.charles.nutrisnap.feature.milestones

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.db.MilestoneEntity
import com.charles.nutrisnap.data.milestone.MilestoneRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class MilestonesViewModel @Inject constructor(
    private val milestoneRepository: MilestoneRepository,
) : ViewModel() {

    val milestones: StateFlow<List<MilestoneEntity>> = milestoneRepository.getAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
}
