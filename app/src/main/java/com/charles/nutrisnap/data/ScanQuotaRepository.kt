package com.charles.nutrisnap.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

const val FREE_MONTHLY_SCAN_LIMIT = 10

data class ScanQuota(
    val period: String = currentQuotaPeriod(),
    val used: Int = 0,
    val limit: Int = FREE_MONTHLY_SCAN_LIMIT,
) {
    val remaining: Int
        get() = (limit - used).coerceAtLeast(0)

    val hasFreeScans: Boolean
        get() = used < limit
}

@Singleton
open class ScanQuotaRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>?,
) {
    private object Keys {
        val PERIOD = stringPreferencesKey("free_scan_quota_period")
        val USED = intPreferencesKey("free_scan_quota_used")
    }

    open val quota: Flow<ScanQuota> by lazy {
        dataStore!!.data.map { prefs ->
            val current = currentQuotaPeriod()
            val savedPeriod = prefs[Keys.PERIOD]
            ScanQuota(
                period = current,
                used = if (savedPeriod == current) prefs[Keys.USED] ?: 0 else 0,
            )
        }
    }

    open suspend fun canUseFreeScan(): Boolean {
        var canUse = false
        dataStore!!.edit { prefs ->
            val current = currentQuotaPeriod()
            val savedPeriod = prefs[Keys.PERIOD]
            val used = if (savedPeriod == current) prefs[Keys.USED] ?: 0 else 0
            canUse = used < FREE_MONTHLY_SCAN_LIMIT
            if (savedPeriod != current) {
                prefs[Keys.PERIOD] = current
                prefs[Keys.USED] = used
            }
        }
        return canUse
    }

    open suspend fun recordFreeScan() {
        dataStore!!.edit { prefs ->
            val current = currentQuotaPeriod()
            val savedPeriod = prefs[Keys.PERIOD]
            val used = if (savedPeriod == current) prefs[Keys.USED] ?: 0 else 0
            prefs[Keys.PERIOD] = current
            prefs[Keys.USED] = (used + 1).coerceAtMost(FREE_MONTHLY_SCAN_LIMIT)
        }
    }
}

fun currentQuotaPeriod(): String =
    SimpleDateFormat("yyyy-MM", Locale.US).format(Date())
