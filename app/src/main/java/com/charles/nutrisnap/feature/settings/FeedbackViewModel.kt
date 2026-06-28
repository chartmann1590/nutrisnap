package com.charles.nutrisnap.feature.settings

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.charles.nutrisnap.data.feedback.BugReportRepo
import com.charles.nutrisnap.data.feedback.DiagnosticsHelper
import com.charles.nutrisnap.data.feedback.GithubApi
import com.charles.nutrisnap.data.feedback.ImageHelper
import com.charles.nutrisnap.data.feedback.StoredReport
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class FeedbackUiState(
    val configError: String? = null,
    val bugReports: List<StoredReport> = emptyList(),
    val showReportDialog: Boolean = false,
    val reportTitle: String = "",
    val reportDescription: String = "",
    val includeDiagnostics: Boolean = true,
    val reporterName: String = "",
    val reporterEmail: String = "",
    val screenshotUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val submitError: String? = null,
    val submitSuccess: Boolean = false,
    val showIssueDialog: Boolean = false,
    val selectedIssueNumber: Int? = null,
    val issueTitle: String = "",
    val issueStatus: String = "",
    val issueHtmlUrl: String = "",
    val issueBody: String? = null,
    val comments: List<IssueCommentUi> = emptyList(),
    val isLoadingIssue: Boolean = false,
    val issueError: String? = null,
    val replyText: String = "",
    val replyScreenshotUri: Uri? = null,
    val isPostingReply: Boolean = false,
    val replyError: String? = null,
)

data class IssueCommentUi(
    val author: String,
    val body: String,
    val createdAt: String,
)

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    application: Application,
    private val githubApi: GithubApi,
    private val bugReportRepo: BugReportRepo,
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(FeedbackUiState())
    val state: StateFlow<FeedbackUiState> = _state.asStateFlow()

    init {
        _state.update { it.copy(configError = githubApi.configError) }
        viewModelScope.launch {
            bugReportRepo.bugReports.collect { reports ->
                _state.update { it.copy(bugReports = reports) }
            }
        }
    }

    fun showReportDialog() {
        _state.update {
            it.copy(
                showReportDialog = true,
                reportTitle = "",
                reportDescription = "",
                includeDiagnostics = true,
                reporterName = "",
                reporterEmail = "",
                screenshotUri = null,
                submitError = null,
                submitSuccess = false,
                isSubmitting = false,
            )
        }
    }

    fun hideReportDialog() {
        _state.update { it.copy(showReportDialog = false) }
    }

    fun updateReportTitle(title: String) {
        _state.update { it.copy(reportTitle = title) }
    }

    fun updateReportDescription(desc: String) {
        _state.update { it.copy(reportDescription = desc) }
    }

    fun updateIncludeDiagnostics(include: Boolean) {
        _state.update { it.copy(includeDiagnostics = include) }
    }

    fun updateReporterName(name: String) {
        _state.update { it.copy(reporterName = name) }
    }

    fun updateReporterEmail(email: String) {
        _state.update { it.copy(reporterEmail = email) }
    }

    fun updateScreenshotUri(uri: Uri?) {
        _state.update { it.copy(screenshotUri = uri) }
    }

    fun submitReport() {
        val s = _state.value
        if (s.reportTitle.isBlank() || s.reportDescription.isBlank()) return
        if (s.isSubmitting) return
        if (!githubApi.isConfigured) {
            _state.update { it.copy(submitError = githubApi.configError ?: "GitHub not configured") }
            return
        }

        _state.update { it.copy(isSubmitting = true, submitError = null, submitSuccess = false) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()
                val diagnostics = if (s.includeDiagnostics) {
                    DiagnosticsHelper.collect(context)
                } else null

                val screenshotMd = if (s.screenshotUri != null) {
                    val base64 = withContext(Dispatchers.IO) {
                        ImageHelper.uriToBase64(context, s.screenshotUri!!).getOrThrow()
                    }
                    val filename = githubApi.generateAssetFilename()
                    val uploadResult = withContext(Dispatchers.IO) {
                        githubApi.uploadAsset(filename, base64)
                    }
                    val downloadUrl = uploadResult.content?.downloadUrl
                    if (downloadUrl != null) {
                        "\n\n## Attachment\n\n![Screenshot]($downloadUrl)"
                    } else ""
                } else ""

                val body = buildString {
                    appendLine("## Description")
                    appendLine()
                    appendLine(s.reportDescription)
                    appendLine()
                    appendLine("## Contact Info")
                    appendLine()
                    appendLine("- Name: ${s.reporterName.ifBlank { "Not provided" }}")
                    appendLine("- Email: ${s.reporterEmail.ifBlank { "Not provided" }}")
                    append(screenshotMd)
                    if (diagnostics != null) {
                        appendLine()
                        appendLine()
                        append(diagnostics)
                    }
                }

                val issue = withContext(Dispatchers.IO) {
                    githubApi.createIssue(
                        title = "[Feedback] ${s.reportTitle}",
                        body = body,
                    )
                }

                val report = StoredReport(
                    number = issue.number,
                    title = s.reportTitle,
                    status = issue.state,
                    createdAt = issue.createdAt,
                    htmlUrl = issue.htmlUrl,
                )
                bugReportRepo.saveReport(report)

                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitSuccess = true,
                        showReportDialog = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSubmitting = false,
                        submitError = e.message ?: "Failed to submit report",
                    )
                }
            }
        }
    }

    fun dismissSuccess() {
        _state.update { it.copy(submitSuccess = false) }
    }

    fun openIssue(report: StoredReport) {
        _state.update {
            it.copy(
                showIssueDialog = true,
                selectedIssueNumber = report.number,
                issueTitle = report.title,
                issueStatus = report.status,
                issueHtmlUrl = report.htmlUrl,
                issueBody = null,
                comments = emptyList(),
                isLoadingIssue = true,
                issueError = null,
                replyText = "",
                replyScreenshotUri = null,
                replyError = null,
            )
        }
        refreshIssue(report.number)
    }

    private fun refreshIssue(number: Int) {
        if (!githubApi.isConfigured) {
            _state.update { it.copy(isLoadingIssue = false, issueError = githubApi.configError) }
            return
        }
        _state.update { it.copy(isLoadingIssue = true, issueError = null) }
        viewModelScope.launch {
            try {
                val issue = withContext(Dispatchers.IO) { githubApi.getIssue(number) }
                val comments = withContext(Dispatchers.IO) { githubApi.getComments(number) }

                bugReportRepo.saveReport(
                    StoredReport(
                        number = issue.number,
                        title = _state.value.issueTitle,
                        status = issue.state,
                        createdAt = issue.createdAt,
                        htmlUrl = issue.htmlUrl,
                    )
                )

                _state.update {
                    it.copy(
                        issueTitle = issue.title.removePrefix("[Feedback] "),
                        issueStatus = issue.state,
                        issueHtmlUrl = issue.htmlUrl,
                        issueBody = issue.body,
                        comments = comments.map { c ->
                            IssueCommentUi(
                                author = c.user.login,
                                body = c.body,
                                createdAt = c.createdAt,
                            )
                        },
                        isLoadingIssue = false,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoadingIssue = false, issueError = e.message ?: "Failed to load issue")
                }
            }
        }
    }

    fun hideIssueDialog() {
        _state.update { it.copy(showIssueDialog = false) }
    }

    fun updateReplyText(text: String) {
        _state.update { it.copy(replyText = text) }
    }

    fun updateReplyScreenshotUri(uri: Uri?) {
        _state.update { it.copy(replyScreenshotUri = uri) }
    }

    fun postReply() {
        val s = _state.value
        if (s.replyText.isBlank() && s.replyScreenshotUri == null) {
            _state.update { it.copy(replyError = "Add text or a screenshot to your reply") }
            return
        }
        if (s.isPostingReply) return
        val issueNumber = s.selectedIssueNumber ?: return
        if (!githubApi.isConfigured) {
            _state.update { it.copy(replyError = githubApi.configError) }
            return
        }

        _state.update { it.copy(isPostingReply = true, replyError = null) }

        viewModelScope.launch {
            try {
                val context = getApplication<Application>()

                val screenshotMd = if (s.replyScreenshotUri != null) {
                    val base64 = withContext(Dispatchers.IO) {
                        ImageHelper.uriToBase64(context, s.replyScreenshotUri!!).getOrThrow()
                    }
                    val filename = githubApi.generateAssetFilename("comment")
                    val uploadResult = withContext(Dispatchers.IO) {
                        githubApi.uploadAsset(filename, base64)
                    }
                    val downloadUrl = uploadResult.content?.downloadUrl
                    if (downloadUrl != null) {
                        "\n\n## Attachment\n\n![Screenshot]($downloadUrl)"
                    } else ""
                } else ""

                val body = buildString {
                    if (s.replyText.isNotBlank()) {
                        appendLine("## Reply")
                        appendLine()
                        appendLine(s.replyText)
                    }
                    append(screenshotMd)
                }

                withContext(Dispatchers.IO) {
                    githubApi.postComment(issueNumber, body)
                }

                val comments = withContext(Dispatchers.IO) { githubApi.getComments(issueNumber) }
                _state.update {
                    it.copy(
                        comments = comments.map { c ->
                            IssueCommentUi(
                                author = c.user.login,
                                body = c.body,
                                createdAt = c.createdAt,
                            )
                        },
                        isPostingReply = false,
                        replyText = "",
                        replyScreenshotUri = null,
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isPostingReply = false,
                        replyError = e.message ?: "Failed to post reply",
                    )
                }
            }
        }
    }
}
