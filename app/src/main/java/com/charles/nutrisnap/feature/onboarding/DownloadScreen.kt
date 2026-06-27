package com.charles.nutrisnap.feature.onboarding

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.charles.nutrisnap.data.ModelState
import com.charles.nutrisnap.ui.components.InfoPill
import com.charles.nutrisnap.ui.components.Pip
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.theme.Berry
import com.charles.nutrisnap.ui.theme.Mango
import com.charles.nutrisnap.ui.theme.NutriTheme

@Composable
fun DownloadScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DownloadViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val verified by viewModel.verified.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (state !is ModelState.Ready) viewModel.start()
    }
    LaunchedEffect(verified) {
        if (verified) onComplete()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Pip(size = 132.dp, animated = true)
        Spacer(Modifier.height(18.dp))
        Text(
            "Teaching your phone\nto see food…",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "Downloading the Gemma 4 brain so NutriSnap works fully offline — no photos ever leave your phone.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(26.dp))

        when (val s = state) {
            is ModelState.Downloading -> ProgressBlock(s.percent, s.bytesSoFar, s.totalBytes)
            is ModelState.Verifying -> {
                Text(
                    "Verifying model…",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Spacer(Modifier.height(8.dp))
                ProgressBlock(100, 0, 0)
            }
            is ModelState.Failed -> {
                Text(s.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error)
                Spacer(Modifier.height(16.dp))
                PrimaryButton(text = "Try again", onClick = viewModel::retry, modifier = Modifier.fillMaxWidth())
            }
            is ModelState.Ready -> ProgressBlock(100, 0, 0)
            ModelState.NotDownloaded -> ProgressBlock(0, 0, 0)
        }

        Spacer(Modifier.height(18.dp))
        InfoPill(text = "📶 On Wi-Fi · safe to download")
        Spacer(Modifier.height(12.dp))
        Text(
            "You can keep using your phone — we'll keep going in the background.",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ProgressBlock(percent: Int, soFar: Long, total: Long) {
    val animated by animateFloatAsState(
        targetValue = percent / 100f,
        animationSpec = tween(500),
        label = "dl",
    )
    Column(Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(16.dp)
                .clip(RoundedCornerShape(50))
                .background(NutriTheme.colors.ringTrack),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(animated)
                    .height(16.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Brush.horizontalGradient(listOf(Mango, Berry))),
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("$percent%", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (total > 0) {
                Text(
                    "${gb(soFar)} / ${gb(total)} GB",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun gb(bytes: Long): String = String.format("%.1f", bytes / 1_000_000_000.0)
