package com.charles.nutrisnap.feature.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.MealRepository
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.components.SecondaryButton
import com.charles.nutrisnap.ui.components.Stepper
import dagger.hilt.android.lifecycle.HiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditMealState(
    val name: String = "",
    val kcal: Int = 0,
    val proteinG: Int = 0,
    val carbsG: Int = 0,
    val fatG: Int = 0,
    val mealType: MealType = MealType.LUNCH,
    val loaded: Boolean = false,
)

@HiltViewModel
class EditMealViewModel @Inject constructor(
    private val mealRepository: MealRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(EditMealState())
    val state: StateFlow<EditMealState> = _state.asStateFlow()

    private var mealId: Long = 0

    fun load(mealId: Long) {
        this.mealId = mealId
        viewModelScope.launch {
            _state.value = _state.value.copy(loaded = true)
        }
    }

    fun delete() {
        viewModelScope.launch {
            mealRepository.deleteMeal(mealId)
        }
    }
}

@Composable
fun EditMealScreen(
    mealId: Long,
    onDeleted: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: EditMealViewModel = hiltViewModel(),
) {
    LaunchedEffect(mealId) {
        viewModel.load(mealId)
    }

    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Text("Edit meal", style = MaterialTheme.typography.headlineMedium, modifier = Modifier.weight(1f))
            IconButton(
                onClick = {
                    viewModel.delete()
                    onDeleted()
                },
            ) {
                Icon(Icons.Rounded.Delete, contentDescription = "Delete meal")
            }
        }

        Spacer(Modifier.height(12.dp))

        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(state.name, style = MaterialTheme.typography.titleLarge)

            Spacer(Modifier.height(16.dp))

            NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
                Text("CALORIES", style = MaterialTheme.typography.labelMedium)
                Stepper("${state.kcal}", "kcal", {}, {})
            }

            Spacer(Modifier.height(16.dp))

            PrimaryButton(text = "Save changes", onClick = onClose, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            SecondaryButton(text = "Discard", onClick = onClose, modifier = Modifier.fillMaxWidth())
        }
    }
}
