package com.charles.nutrisnap.data.feedback

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.charles.nutrisnap.di.FeedbackDataStore
import com.charles.nutrisnap.di.FeedbackJson
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BugReportRepo @Inject constructor(
    @FeedbackDataStore private val dataStore: DataStore<Preferences>,
    @FeedbackJson private val json: Json,
) {
    private object Keys {
        val BUG_REPORTS = stringPreferencesKey("bug_reports_list")
    }

    val bugReports: Flow<List<StoredReport>> = dataStore.data.map { prefs ->
        val raw = prefs[Keys.BUG_REPORTS]
        if (!raw.isNullOrBlank()) {
            try {
                json.decodeFromString<List<StoredReport>>(raw)
                    .sortedByDescending { it.createdAt }
            } catch (_: Exception) {
                emptyList()
            }
        } else {
            emptyList()
        }
    }

    suspend fun saveReport(report: StoredReport) {
        dataStore.edit { prefs ->
            val raw = prefs[Keys.BUG_REPORTS]
            val list = if (!raw.isNullOrBlank()) {
                try {
                    json.decodeFromString<List<StoredReport>>(raw).toMutableList()
                } catch (_: Exception) {
                    mutableListOf()
                }
            } else {
                mutableListOf()
            }
            val idx = list.indexOfFirst { it.number == report.number }
            if (idx >= 0) {
                list[idx] = report
            } else {
                list.add(report)
            }
            prefs[Keys.BUG_REPORTS] = json.encodeToString(list.sortedByDescending { it.createdAt })
        }
    }

    suspend fun updateReports(reports: List<StoredReport>) {
        dataStore.edit { prefs ->
            prefs[Keys.BUG_REPORTS] = json.encodeToString(reports.sortedByDescending { it.createdAt })
        }
    }

    suspend fun getReportsList(): List<StoredReport> {
        val raw = dataStore.data.first()[Keys.BUG_REPORTS]
        if (!raw.isNullOrBlank()) {
            return try {
                json.decodeFromString<List<StoredReport>>(raw)
            } catch (_: Exception) {
                emptyList()
            }
        }
        return emptyList()
    }
}
