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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
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
import com.charles.nutrisnap.util.findActivity

@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val pipSoundsEnabled by viewModel.pipSoundsEnabled.collectAsStateWithLifecycle()
    val pipVoiceEnabled by viewModel.pipVoiceEnabled.collectAsStateWithLifecycle()
    val activity = LocalContext.current.findActivity()

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

        // About You — editable weight
        var editingWeight by remember { mutableStateOf(false) }
        var weightDraft by remember(state.latestWeightKg) {
            mutableStateOf(state.latestWeightKg?.let { String.format("%.1f", it) } ?: "")
        }
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("About You", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    if (editingWeight) {
                        weightDraft.toDoubleOrNull()?.let { viewModel.saveWeight(it) }
                    }
                    editingWeight = !editingWeight
                }) { Text(if (editingWeight) "Save" else "Edit") }
            }
            Spacer(Modifier.height(8.dp))
            if (editingWeight) {
                OutlinedTextField(
                    value = weightDraft,
                    onValueChange = { weightDraft = it },
                    label = { Text("Current weight (kg)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Text(
                    state.latestWeightKg?.let { String.format("%.1f kg", it) } ?: "No weight logged yet",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        // Daily goal — editable
        var editingGoal by remember { mutableStateOf(false) }
        var calDraft by remember(state.goal) { mutableStateOf(state.goal?.calories?.toString() ?: "") }
        var proteinDraft by remember(state.goal) { mutableStateOf(state.goal?.proteinG?.toString() ?: "") }
        var carbsDraft by remember(state.goal) { mutableStateOf(state.goal?.carbsG?.toString() ?: "") }
        var fatDraft by remember(state.goal) { mutableStateOf(state.goal?.fatG?.toString() ?: "") }
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Daily goal", style = MaterialTheme.typography.titleMedium)
                TextButton(onClick = {
                    if (editingGoal) {
                        val cal = calDraft.toIntOrNull() ?: 0
                        val p = proteinDraft.toIntOrNull() ?: 0
                        val c = carbsDraft.toIntOrNull() ?: 0
                        val f = fatDraft.toIntOrNull() ?: 0
                        if (cal > 0) viewModel.saveGoal(cal, p, c, f)
                    }
                    editingGoal = !editingGoal
                }) { Text(if (editingGoal) "Save" else "Edit") }
            }
            Spacer(Modifier.height(8.dp))
            if (editingGoal) {
                OutlinedTextField(
                    value = calDraft,
                    onValueChange = { calDraft = it },
                    label = { Text("Calories (kcal)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = proteinDraft,
                        onValueChange = { proteinDraft = it },
                        label = { Text("Protein (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = carbsDraft,
                        onValueChange = { carbsDraft = it },
                        label = { Text("Carbs (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = fatDraft,
                        onValueChange = { fatDraft = it },
                        label = { Text("Fat (g)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
            } else {
                state.goal?.let { goal ->
                    Text("${goal.calories} kcal", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(4.dp))
                    Text("Protein ${goal.proteinG}g  Carbs ${goal.carbsG}g  Fat ${goal.fatG}g", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } ?: Text("No goal set", style = MaterialTheme.typography.bodyMedium)
            }
        }

        Spacer(Modifier.height(16.dp))

        // Units
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Units", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            UnitToggle(state.goal?.let { com.charles.nutrisnap.data.UnitSystem.METRIC } ?: com.charles.nutrisnap.data.UnitSystem.METRIC) { _ -> }
        }

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

        // Premium
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Premium", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Text(
                if (state.premiumEntitlement.isPremium) {
                    "Active - unlimited AI scans"
                } else {
                    "Free plan - 10 AI scans each month"
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            state.billingMessage?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = viewModel::restorePurchases) {
                Text("Restore purchases")
            }
        }

        Spacer(Modifier.height(16.dp))

        // Privacy & Diagnostics
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Privacy & Diagnostics", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))

            SettingToggle(
                label = "Crashlytics (crash reports)",
                checked = state.crashlyticsEnabled,
                onCheckedChange = viewModel::setCrashlyticsEnabled,
            )
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                label = "Performance Monitoring",
                checked = state.performanceEnabled,
                onCheckedChange = viewModel::setPerformanceEnabled,
            )
            Spacer(Modifier.height(8.dp))
            SettingToggle(
                label = "Analytics (usage data)",
                checked = state.analyticsEnabled,
                onCheckedChange = viewModel::setAnalyticsEnabled,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                "Disabling these does not affect your tracking — only diagnostic data stops being sent.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(12.dp))
            TextButton(onClick = viewModel::resetFirebaseId) {
                Text("Reset analytics identifier")
            }
            activity?.let { act ->
                TextButton(onClick = { viewModel.manageAdConsent(act) }) {
                    Text("Manage ad consent")
                }
            }
        }

        Spacer(Modifier.height(16.dp))

        // Pip Personality
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text("Pip Personality", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Pip Sounds 🔔", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Play sounds when Pip reacts",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = pipSoundsEnabled, onCheckedChange = { viewModel.setPipSoundsEnabled(it) })
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.weight(1f)) {
                    Text("Pip's Voice 🗣️", style = MaterialTheme.typography.bodyMedium)
                    Text(
                        "Let Pip read responses aloud",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(checked = pipVoiceEnabled, onCheckedChange = { viewModel.setPipVoiceEnabled(it) })
            }
        }

        Spacer(Modifier.height(16.dp))

        SupportAndFeedbackSection()

        Spacer(Modifier.height(16.dp))

        Text("All data stays on this device. Powered by Gemma 4.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun SettingToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
