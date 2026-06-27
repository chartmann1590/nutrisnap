package com.charles.nutrisnap.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.ModelState
import com.charles.nutrisnap.data.ModelVariant
import com.charles.nutrisnap.data.ThemeMode
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.components.SegmentedToggle
import com.charles.nutrisnap.ui.components.UnitToggle

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp)
            .verticalScroll(rememberScrollState()),
    ) {
        Text("Settings", style = MaterialTheme.typography.headlineLarge)
        Spacer(Modifier.height(24.dp))

        // On-device AI card
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("On-device AI", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                "Model: ${state.currentVariant.name}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(12.dp))

            SegmentedToggle(
                options = ModelVariant.entries.map { it.name to it.name },
                selected = state.currentVariant.name,
                onChange = { name ->
                    ModelVariant.entries.find { it.name == name }?.let { viewModel.setVariant(it) }
                },
            )

            Spacer(Modifier.height(12.dp))

            when (val ms = state.modelState) {
                is ModelState.Ready -> {
                    Text("Model ready", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is ModelState.Downloading -> {
                    Text("Downloading: ${ms.percent}%", style = MaterialTheme.typography.bodySmall)
                }
                is ModelState.Verifying -> {
                    Text("Verifying model…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                is ModelState.Failed -> {
                    Text(ms.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    Spacer(Modifier.height(8.dp))
                    PrimaryButton(text = "Retry", onClick = viewModel::retryDownload)
                }
                is ModelState.NotDownloaded -> {
                    PrimaryButton(text = "Start download", onClick = viewModel::retryDownload)
                }
            }

            Spacer(Modifier.height(8.dp))
            Text("Your food is analyzed entirely on device � no photos ever leave your phone.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        Spacer(Modifier.height(16.dp))

        // Daily goal
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Daily goal", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            state.goal?.let { goal ->
                Text("${goal.calories} kcal", style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(4.dp))
                Text("Protein ${goal.proteinG}g � Carbs ${goal.carbsG}g � Fat ${goal.fatG}g", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } ?: Text("No goal set", style = MaterialTheme.typography.bodyMedium)
        }

        Spacer(Modifier.height(16.dp))

        // Units
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Units", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            UnitToggle(state.goal?.let { com.charles.nutrisnap.data.UnitSystem.METRIC } ?: com.charles.nutrisnap.data.UnitSystem.METRIC) { _ -> }
        }

        Spacer(Modifier.height(16.dp))

        // Dark mode
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Appearance", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            SegmentedToggle(
                options = ThemeMode.entries.map { it.name to it.name },
                selected = state.themeMode.name,
                onChange = { name ->
                    ThemeMode.entries.find { it.name == name }?.let { viewModel.setThemeMode(it) }
                },
            )
        }

        Spacer(Modifier.height(16.dp))

        Text("All data stays on this device. Powered by Gemma 4.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}