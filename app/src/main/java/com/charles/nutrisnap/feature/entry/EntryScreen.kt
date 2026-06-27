package com.charles.nutrisnap.feature.entry

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.ai.FoodEstimate
import com.charles.nutrisnap.data.db.MealSource
import com.charles.nutrisnap.data.db.MealType
import com.charles.nutrisnap.ui.components.ConfidencePill
import com.charles.nutrisnap.ui.components.ErrorState
import com.charles.nutrisnap.ui.components.LoadingState
import com.charles.nutrisnap.ui.components.MacroTile
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.components.SegmentedToggle
import com.charles.nutrisnap.ui.components.Stepper
import com.charles.nutrisnap.ui.theme.NutriTheme

@Composable
fun EntryScreen(
    mode: String = "manual",
    bitmapKey: String? = null,
    onLogged: () -> Unit = {},
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: EntryViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    var entryMode by remember { mutableStateOf("describe") }
    var description by remember { mutableStateOf("") }
    var editableKcal by remember { mutableStateOf("") }
    var editableProtein by remember { mutableStateOf("") }
    var editableCarbs by remember { mutableStateOf("") }
    var editableFat by remember { mutableStateOf("") }
    var selectedMealType by remember { mutableStateOf(MealType.LUNCH) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var attemptedEstimate by remember { mutableStateOf(false) }

    val estimate = when (val s = state) {
        is EntryUiState.EstimateReady -> s.estimate
        else -> null
    }

    LaunchedEffect(estimate) {
        estimate?.let {
            editableKcal = it.kcal.toString()
            editableProtein = it.proteinG.toString()
            editableCarbs = it.carbsG.toString()
            editableFat = it.fatG.toString()
        }
    }

    LaunchedEffect(state) {
        if (attemptedEstimate && state is EntryUiState.Idle) {
            errorMessage = "Could not estimate your meal. Try a different description."
            attemptedEstimate = false
        }
        if (state is EntryUiState.EstimateReady) {
            errorMessage = null
            attemptedEstimate = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                EntryEvent.Logged -> onLogged()
            }
        }
    }

    LaunchedEffect(description) {
        errorMessage = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onClose) {
                Icon(Icons.Rounded.ArrowBack, contentDescription = "Back")
            }
            Text("Add food", style = MaterialTheme.typography.headlineMedium)
        }

        Spacer(Modifier.height(12.dp))

        SegmentedToggle(
            options = listOf("describe" to "Describe", "barcode" to "Barcode"),
            selected = entryMode,
            onChange = { entryMode = it },
        )

        Spacer(Modifier.height(16.dp))

        when (entryMode) {
            "describe" -> {
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    placeholder = { Text("Describe your meal...") },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            if (description.isNotBlank()) {
                                attemptedEstimate = true
                                errorMessage = null
                                viewModel.estimateFromText(description)
                            }
                        }
                    ),
                    trailingIcon = {
                        IconButton(onClick = {
                            if (description.isNotBlank()) {
                                attemptedEstimate = true
                                errorMessage = null
                                viewModel.estimateFromText(description)
                            }
                        }) {
                            Icon(Icons.Rounded.Search, contentDescription = "Estimate")
                        }
                    },
                    singleLine = true,
                )

                Spacer(Modifier.height(12.dp))

                if (errorMessage != null) {
                    ErrorState(
                        title = "Estimation failed",
                        message = errorMessage!!,
                        onRetry = {
                            errorMessage = null
                            if (description.isNotBlank()) {
                                attemptedEstimate = true
                                viewModel.estimateFromText(description)
                            }
                        },
                        modifier = Modifier,
                    )
                }

                when (val s = state) {
                    is EntryUiState.Estimating -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            Pip(size = 64.dp, animated = true)
                        }
                    }
                    is EntryUiState.EstimateReady -> {
                        EstimateContent(
                            estimate = s.estimate,
                            editableKcal = editableKcal,
                            editableProtein = editableProtein,
                            editableCarbs = editableCarbs,
                            editableFat = editableFat,
                            selectedMealType = selectedMealType,
                            onKcalChange = { editableKcal = it },
                            onProteinChange = { editableProtein = it },
                            onCarbsChange = { editableCarbs = it },
                            onFatChange = { editableFat = it },
                            onMealTypeChange = { selectedMealType = it },
                            onLog = {
                                val k = editableKcal.toIntOrNull() ?: s.estimate.kcal
                                val p = editableProtein.toIntOrNull() ?: s.estimate.proteinG
                                val c = editableCarbs.toIntOrNull() ?: s.estimate.carbsG
                                val f = editableFat.toIntOrNull() ?: s.estimate.fatG
                                viewModel.logMeal(
                                    s.estimate.copy(kcal = k, proteinG = p, carbsG = c, fatG = f),
                                    selectedMealType,
                                    source = if (bitmapKey != null) MealSource.SCAN else MealSource.MANUAL,
                                )
                            },
                        )
                    }
                    is EntryUiState.Logging -> {
                        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            LoadingState(modifier = Modifier)
                        }
                    }
                    is EntryUiState.Done -> {
                        LaunchedEffect(Unit) { onLogged() }
                    }
                    else -> {}
                }
            }
            "barcode" -> {
                Text(
                    "Point camera at barcode",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun EstimateContent(
    estimate: FoodEstimate,
    editableKcal: String,
    editableProtein: String,
    editableCarbs: String,
    editableFat: String,
    selectedMealType: MealType,
    onKcalChange: (String) -> Unit,
    onProteinChange: (String) -> Unit,
    onCarbsChange: (String) -> Unit,
    onFatChange: (String) -> Unit,
    onMealTypeChange: (MealType) -> Unit,
    onLog: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState()),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                estimate.name,
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.weight(1f),
            )
            ConfidencePill(percent = (estimate.confidence * 100).toInt())
        }

        Spacer(Modifier.height(16.dp))

        NutriCard(cornerRadius = 24.dp, padding = 18.dp, modifier = Modifier.fillMaxWidth()) {
            Text("CALORIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Stepper(
                value = editableKcal,
                unit = "kcal",
                onMinus = { onKcalChange((editableKcal.toIntOrNull()?.let { (it - 10).coerceAtLeast(0) } ?: 0).toString()) },
                onPlus = { onKcalChange((editableKcal.toIntOrNull()?.let { it + 10 } ?: 0).toString()) },
            )
        }

        Spacer(Modifier.height(12.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            MacroTile("Protein", "${editableProtein}g", NutriTheme.colors.protein, Modifier.weight(1f))
            MacroTile("Carbs", "${editableCarbs}g", NutriTheme.colors.carbs, Modifier.weight(1f))
            MacroTile("Fat", "${editableFat}g", NutriTheme.colors.fat, Modifier.weight(1f))
        }

        Spacer(Modifier.height(16.dp))

        Text("MEAL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            MealType.entries.forEach { type ->
                val isSelected = selectedMealType == type
                Box(
                    Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface)
                        .clickable { onMealTypeChange(type) }
                        .padding(vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        type.name.lowercase().replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelLarge,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(24.dp))

        PrimaryButton(text = "Log meal", onClick = onLog, modifier = Modifier.fillMaxWidth())
    }
}
