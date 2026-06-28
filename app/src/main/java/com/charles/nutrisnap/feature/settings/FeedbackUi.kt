package com.charles.nutrisnap.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import com.charles.nutrisnap.ui.components.NutriCard
import com.charles.nutrisnap.ui.components.PrimaryButton
import com.charles.nutrisnap.ui.theme.Mint
import com.charles.nutrisnap.ui.theme.NutriTheme

@Composable
fun SupportAndFeedbackSection(
    viewModel: FeedbackViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    NutriCard(cornerRadius = 20.dp, padding = 16.dp, modifier = Modifier.fillMaxWidth()) {
        Text("Support & Feedback", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "Report a problem or send feedback. Submitted reports are posted to this app's GitHub issue tracker.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(12.dp))

        PrimaryButton(
            text = "Report a Problem",
            onClick = { viewModel.showReportDialog() },
            modifier = Modifier.fillMaxWidth(),
        )

        if (state.bugReports.isNotEmpty()) {
            Spacer(Modifier.height(16.dp))
            Text(
                "Submitted Reports",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(8.dp))
            state.bugReports.forEach { report ->
                ReportRow(
                    report = report,
                    onClick = { viewModel.openIssue(report) },
                )
                Spacer(Modifier.height(6.dp))
            }
        }
    }

    if (state.showReportDialog) {
        ReportDialog(viewModel = viewModel, state = state)
    }

    if (state.showIssueDialog && state.selectedIssueNumber != null) {
        IssueDetailsSheet(viewModel = viewModel, state = state)
    }
}

@Composable
private fun ReportRow(
    report: com.charles.nutrisnap.data.feedback.StoredReport,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            Icons.Default.BugReport,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = report.title,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "#${report.number} \u00B7 ${report.createdAt.take(10)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(8.dp))
        val isOpen = report.status == "open"
        val badgeColor = if (isOpen) Mint else MaterialTheme.colorScheme.error
        val badgeText = if (isOpen) "Open" else "Closed"
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(badgeColor.copy(alpha = 0.15f))
                .padding(horizontal = 8.dp, vertical = 3.dp),
        ) {
            Text(
                badgeText,
                style = MaterialTheme.typography.labelSmall,
                color = badgeColor,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun ReportDialog(
    viewModel: FeedbackViewModel,
    state: FeedbackUiState,
) {
    val scrollState = rememberScrollState()
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        viewModel.updateScreenshotUri(uri)
    }

    androidx.compose.material3.AlertDialog(
        onDismissRequest = { viewModel.hideReportDialog() },
        title = { Text("Report a Problem") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState),
            ) {
                // Warning box
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(12.dp),
                ) {
                    Text(
                        "Your report will be submitted to this app\u2019s GitHub issue tracker. " +
                            "Do not include passwords, private keys, medical information, " +
                            "financial information, or anything you do not want visible to " +
                            "the repository maintainers. If this repository is public, your " +
                            "report may be publicly visible.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = state.reportTitle,
                    onValueChange = viewModel::updateReportTitle,
                    label = { Text("Title / Subject *") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.reportDescription,
                    onValueChange = viewModel::updateReportDescription,
                    label = { Text("Description *") },
                    minLines = 3,
                    maxLines = 6,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = state.includeDiagnostics,
                        onCheckedChange = viewModel::updateIncludeDiagnostics,
                        enabled = !state.isSubmitting,
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Include phone/app diagnostics",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.reporterName,
                    onValueChange = viewModel::updateReporterName,
                    label = { Text("Name (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                )

                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.reporterEmail,
                    onValueChange = viewModel::updateReporterEmail,
                    label = { Text("Email (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isSubmitting,
                )

                Spacer(Modifier.height(12.dp))

                // Screenshot section
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        enabled = !state.isSubmitting,
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Attach screenshot")
                    }

                    if (state.screenshotUri != null) {
                        IconButton(onClick = { viewModel.updateScreenshotUri(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image")
                        }
                    }
                }

                if (state.screenshotUri != null) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = state.screenshotUri,
                        contentDescription = "Screenshot preview",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Screenshots may contain private information. Review before submitting.",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                if (state.submitError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.submitError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                if (state.configError != null && !state.isSubmitting) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.configError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { viewModel.hideReportDialog() },
                        enabled = !state.isSubmitting,
                    ) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        onClick = { viewModel.submitReport() },
                        enabled = state.reportTitle.isNotBlank()
                            && state.reportDescription.isNotBlank()
                            && !state.isSubmitting
                            && state.configError == null,
                    ) {
                        if (state.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                            )
                            Spacer(Modifier.width(8.dp))
                        }
                        Text("Submit")
                    }
                }
            }
        },
        confirmButton = {},
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun IssueDetailsSheet(
    viewModel: FeedbackViewModel,
    state: FeedbackUiState,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri: Uri? ->
        viewModel.updateReplyScreenshotUri(uri)
    }

    ModalBottomSheet(
        onDismissRequest = { viewModel.hideIssueDialog() },
        sheetState = sheetState,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            // Issue header
            Text(
                text = state.issueTitle,
                style = MaterialTheme.typography.titleLarge,
            )
            Spacer(Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val isOpen = state.issueStatus == "open"
                val badgeColor = if (isOpen) Mint else MaterialTheme.colorScheme.error
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(badgeColor.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                ) {
                    Text(
                        if (isOpen) "Open" else "Closed",
                        style = MaterialTheme.typography.labelSmall,
                        color = badgeColor,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    "#${state.selectedIssueNumber}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (state.issueHtmlUrl.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = state.issueHtmlUrl,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.height(16.dp))

            // Issue body
            if (state.isLoadingIssue) {
                Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (state.issueError != null) {
                Text(
                    state.issueError,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            } else {
                // Comments section
                Text("Comments", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                if (state.comments.isEmpty()) {
                    Text(
                        "No comments yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    state.comments.forEach { comment ->
                        CommentBubble(comment)
                        Spacer(Modifier.height(8.dp))
                    }
                }

                HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp))

                // Reply section
                Text("Add a Reply", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(8.dp))

                OutlinedTextField(
                    value = state.replyText,
                    onValueChange = viewModel::updateReplyText,
                    label = { Text("Your reply") },
                    minLines = 2,
                    maxLines = 4,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isPostingReply,
                )

                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedButton(
                        onClick = { photoPickerLauncher.launch("image/*") },
                        enabled = !state.isPostingReply,
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Screenshot")
                    }

                    if (state.replyScreenshotUri != null) {
                        IconButton(onClick = { viewModel.updateReplyScreenshotUri(null) }) {
                            Icon(Icons.Default.Close, contentDescription = "Remove image")
                        }
                    }
                }

                if (state.replyScreenshotUri != null) {
                    Spacer(Modifier.height(8.dp))
                    AsyncImage(
                        model = state.replyScreenshotUri,
                        contentDescription = "Reply screenshot",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(120.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop,
                    )
                }

                if (state.replyError != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        state.replyError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Spacer(Modifier.height(12.dp))

                Button(
                    onClick = { viewModel.postReply() },
                    enabled = (state.replyText.isNotBlank() || state.replyScreenshotUri != null)
                        && !state.isPostingReply,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (state.isPostingReply) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Send Reply")
                }
            }
        }
    }
}

@Composable
private fun CommentBubble(comment: IssueCommentUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    comment.author,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    comment.createdAt.take(10),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                comment.body,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}
