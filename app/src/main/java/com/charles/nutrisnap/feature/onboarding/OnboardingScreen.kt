package com.charles.nutrisnap.feature.onboarding

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.UnitSystem
import com.charles.nutrisnap.ui.components.GoalPill
import com.charles.nutrisnap.ui.components.MacroTile
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.OptionCard
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.components.SecondaryButton
import com.charles.nutrisnap.ui.components.SelectChip
import com.charles.nutrisnap.ui.components.Stepper
import com.charles.nutrisnap.ui.components.UnitToggle
import com.charles.nutrisnap.ui.theme.NutriTheme
import kotlin.math.roundToInt


@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val progress by animateFloatAsState(
        targetValue = (state.step + 1f) / ONBOARDING_STEPS,
        animationSpec = tween(400),
        label = "progress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
            .padding(top = 48.dp, bottom = 20.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.weight(1f)) {
                Text(
                    "STEP ${state.step + 1} OF $ONBOARDING_STEPS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(6.dp))
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(50))
                        .background(NutriTheme.colors.ringTrack),
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth(progress)
                            .height(8.dp)
                            .clip(RoundedCornerShape(50))
                            .background(MaterialTheme.colorScheme.primary),
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Pip(size = 48.dp, animated = false)
        }

        Spacer(Modifier.height(18.dp))
        Text(stepTitle(state.step), style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            stepSubtitle(state.step),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Spacer(Modifier.height(16.dp))
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            when (state.step) {
                0 -> StepAboutYou(state, viewModel)
                1 -> StepMeasurements(state, viewModel)
                2 -> StepActivity(state, viewModel)
                3 -> StepGoal(state, viewModel)
                else -> StepSummary(state)
            }
        }

        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (state.step > 0) {
                SecondaryButton(text = "Back", onClick = viewModel::back)
                Spacer(Modifier.width(10.dp))
            }
            PrimaryButton(
                text = if (state.isLastStep) "Start tracking" else "Continue",
                onClick = { if (state.isLastStep) viewModel.finish() else viewModel.next() },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

private fun stepTitle(step: Int) = when (step) {
    0 -> "A little about you"
    1 -> "Your measurements"
    2 -> "How active are you?"
    3 -> "What's your goal?"
    else -> "Your daily plan"
}

private fun stepSubtitle(step: Int) = when (step) {
    0 -> "This helps Pip calculate your daily needs."
    1 -> "Pop in your height and weight."
    2 -> "Pick the option that fits your week."
    3 -> "We'll tune your calories to match."
    else -> "Here's what we'll aim for each day."
}

@Composable
private fun StepAboutYou(state: OnboardingUiState, vm: OnboardingViewModel) {
    Text("BIOLOGICAL SEX", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SelectChip("Male", state.sex == Sex.MALE, Modifier.weight(1f)) { vm.setSex(Sex.MALE) }
        SelectChip("Female", state.sex == Sex.FEMALE, Modifier.weight(1f)) { vm.setSex(Sex.FEMALE) }
    }
    Text("AGE", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    Stepper(value = "${state.age}", unit = "years", onMinus = { vm.setAge(state.age - 1) }, onPlus = { vm.setAge(state.age + 1) })
}

@Composable
private fun StepMeasurements(state: OnboardingUiState, vm: OnboardingViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Text("UNITS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(1f))
        UnitToggle(state.units) { vm.setUnits(it) }
    }
    if (state.units == UnitSystem.METRIC) {
        Text("HEIGHT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Stepper("${state.heightCm}", "cm", { vm.setHeightCm(state.heightCm - 1) }, { vm.setHeightCm(state.heightCm + 1) })
        Text("WEIGHT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Stepper(formatKg(state.weightKg), "kg", { vm.setWeightKg(state.weightKg - 0.5) }, { vm.setWeightKg(state.weightKg + 0.5) })
    } else {
        val totalInches = (state.heightCm / 2.54).roundToInt()
        Text("HEIGHT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Stepper("${totalInches / 12}' ${totalInches % 12}\"", "", { vm.setHeightCm(((totalInches - 1) * 2.54).roundToInt()) }, { vm.setHeightCm(((totalInches + 1) * 2.54).roundToInt()) })
        Text("WEIGHT", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        val lb = (state.weightKg * 2.2046226).roundToInt()
        Stepper("$lb", "lb", { vm.setWeightKg((lb - 1) / 2.2046226) }, { vm.setWeightKg((lb + 1) / 2.2046226) })
    }
}

@Composable
private fun StepActivity(state: OnboardingUiState, vm: OnboardingViewModel) {
    ActivityLevel.entries.forEach { level ->
        OptionCard(
            emoji = level.emoji,
            title = level.label,
            subtitle = level.blurb,
            selected = state.activity == level,
            accent = NutriTheme.colors.protein,
        ) { vm.setActivity(level) }
    }
}

@Composable
private fun StepGoal(state: OnboardingUiState, vm: OnboardingViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Goal.entries.forEach { g ->
            GoalPill(g.label, state.goal == g, Modifier.weight(1f)) { vm.setGoal(g) }
        }
    }
    Spacer(Modifier.height(4.dp))
    val hint = when (state.goal) {
        Goal.LOSE -> "We'll aim for a gentle, sustainable deficit."
        Goal.MAINTAIN -> "We'll keep you steady at maintenance."
        Goal.GAIN -> "We'll add a modest surplus to build up."
    }
    Text(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

@Composable
private fun StepSummary(state: OnboardingUiState) {
    val goal = state.preview
    NutriCard(cornerRadius = 28.dp, padding = 22.dp, modifier = Modifier.fillMaxWidth()) {
        Text("DAILY CALORIES", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text("${goal.calories}", style = MaterialTheme.typography.displayMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            MacroTile("Protein", "${goal.proteinG}g", NutriTheme.colors.protein, Modifier.weight(1f))
            MacroTile("Carbs", "${goal.carbsG}g", NutriTheme.colors.carbs, Modifier.weight(1f))
            MacroTile("Fat", "${goal.fatG}g", NutriTheme.colors.fat, Modifier.weight(1f))
        }
    }
    Text(
        "You can fine-tune this any time in Settings.",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

private fun formatKg(kg: Double): String =
    if (kg % 1.0 == 0.0) "${kg.toInt()}" else String.format("%.1f", kg)