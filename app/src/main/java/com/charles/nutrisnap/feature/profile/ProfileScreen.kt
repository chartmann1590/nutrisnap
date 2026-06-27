package com.charles.nutrisnap.feature.profile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.SecondaryButton
import com.charles.nutrisnap.ui.components.StreakPill
import com.charles.nutrisnap.ui.theme.NutriTheme

@Composable
fun ProfileScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Pip(size = 100.dp, animated = false)
        Spacer(Modifier.height(16.dp))

        if (state.streak > 0) {
            StreakPill(days = state.streak)
            Spacer(Modifier.height(12.dp))
        }

        Text("Daily goal", style = MaterialTheme.typography.titleMedium)
        NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
            Text(state.goalSummary, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
        }

        Spacer(Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Weight", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(state.latestWeight, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                }
            }

            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Streak", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("${state.streak}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = NutriTheme.colors.streak)
                }
            }
        }

        Spacer(Modifier.height(32.dp))
        SecondaryButton(text = "Open settings", onClick = onOpenSettings, modifier = Modifier.fillMaxWidth())
    }
}