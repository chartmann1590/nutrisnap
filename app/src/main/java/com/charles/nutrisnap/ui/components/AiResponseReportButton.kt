package com.charles.nutrisnap.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Flag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import com.charles.nutrisnap.data.report.AiReportApi
import com.charles.nutrisnap.data.report.AiReportCategory
import com.charles.nutrisnap.data.report.AiReportSurface
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.launch

@HiltViewModel
class AiResponseReportViewModel @Inject constructor(private val api: AiReportApi) : ViewModel() {
    val available: Boolean get() = api.isConfigured
    suspend fun send(surface: AiReportSurface, category: AiReportCategory) = api.submitReport(surface, category)
}

@Composable
fun AiResponseReportButton(
    surface: AiReportSurface,
    viewModel: AiResponseReportViewModel = hiltViewModel(),
) {
    var open by remember { mutableStateOf(false) }
    var sending by remember { mutableStateOf(false) }
    var sent by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var category by remember { mutableStateOf(AiReportCategory.INAPPROPRIATE) }
    val scope = rememberCoroutineScope()
    IconButton(onClick = {
        category = AiReportCategory.INAPPROPRIATE
        sent = false
        failed = false
        open = true
    }) {
        Icon(Icons.Rounded.Flag, contentDescription = "Report this AI response")
    }
    if (!open) return
    AlertDialog(
        onDismissRequest = { if (!sending) open = false },
        title = { Text("Report this AI response") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                when {
                    sent -> Text("Thanks. Your report was sent.")
                    !viewModel.available -> Text("Reporting is unavailable in this build.")
                    else -> {
                        Text("This sends the reason, the app version, and which feature you used. It does not send your photo, text, or nutrition estimate.")
                        val reasons = if (surface == AiReportSurface.PIP_CHAT) {
                            listOf(AiReportCategory.INAPPROPRIATE, AiReportCategory.WRONG_NUTRITION, AiReportCategory.OTHER)
                        } else AiReportCategory.entries
                        for (reason in reasons) {
                            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                                RadioButton(selected = category == reason, onClick = { category = reason }, enabled = !sending)
                                Text(reason.label)
                            }
                        }
                        if (failed) Text("Couldn't send the report. Please try again.")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !sending,
                onClick = {
                    if (sent || !viewModel.available) open = false
                    else if (!sending) {
                        sending = true
                        failed = false
                        scope.launch {
                            try {
                                viewModel.send(surface, category)
                                sent = true
                            } catch (_: IOException) {
                                failed = true
                            } finally {
                                sending = false
                            }
                        }
                    }
                },
            ) { Text(if (sent || !viewModel.available) "Done" else if (sending) "Sending…" else "Send report") }
        },
        dismissButton = {
            if (!sent && viewModel.available) TextButton(onClick = { open = false }, enabled = !sending) { Text("Cancel") }
        },
    )
}
