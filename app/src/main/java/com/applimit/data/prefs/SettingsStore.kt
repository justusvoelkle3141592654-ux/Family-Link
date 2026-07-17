package com.applimit.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "applimit_settings")

/**
 * All parent-configurable values. The hard upper bounds live here as code
 * constants so the UI can never exceed them (Prompt Punkt 3: "harte Obergrenze
 * im Code, nicht nur UI-Hinweis").
 */
data class AppSettings(
    val dailyLimitMinutes: Int = DEFAULT_DAILY_LIMIT_MIN,
    val fullLockMinutes: Int = DEFAULT_FULL_LOCK_MIN,
    /**
     * DECISION (Rückfrage 1): default true — every foreground app, including
     * PLUS apps, counts towards the full-device-lock budget. Parent can switch
     * this to "only limited apps".
     */
    val fullLockCountsAllApps: Boolean = true,
    /** DECISION (Rückfrage 3): reset the full lock + counters daily at midnight. */
    val autoResetAtMidnight: Boolean = true,
    /** Day of week the app may be opened. 1=Mon … 7=Sun (java.time). Default Sun. */
    val weeklyOpenDayOfWeek: Int = 7,
    val weeklyOpenStartHour: Int = 18,
    val weeklyOpenEndHour: Int = 20,
    /** ISO week (year*100+week) in which the child last opened the app. */
    val lastOpenIsoWeek: Int = 0,
    /** True once the full-device lock has tripped for the current day. */
    val deviceLockedToday: Boolean = false,
    /** yyyyDDD day-of-year marker of the last midnight reset. */
    val lastResetDayOfYear: Int = 0,
) {
    companion object {
        const val DEFAULT_DAILY_LIMIT_MIN = 60
        const val MAX_DAILY_LIMIT_MIN = 120        // hard cap: 2h
        const val MIN_DAILY_LIMIT_MIN = 5
        const val DEFAULT_FULL_LOCK_MIN = 120       // hard cap: 2h total → full lock
        const val MAX_FULL_LOCK_MIN = 120
    }
}

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            dailyLimitMinutes = (p[DAILY_LIMIT] ?: AppSettings.DEFAULT_DAILY_LIMIT_MIN)
                .coerceIn(AppSettings.MIN_DAILY_LIMIT_MIN, AppSettings.MAX_DAILY_LIMIT_MIN),
            fullLockMinutes = (p[FULL_LOCK] ?: AppSettings.DEFAULT_FULL_LOCK_MIN)
                .coerceAtMost(AppSettings.MAX_FULL_LOCK_MIN),
            fullLockCountsAllApps = p[COUNT_ALL] ?: true,
            autoResetAtMidnight = p[AUTO_RESET] ?: true,
            weeklyOpenDayOfWeek = p[OPEN_DOW] ?: 7,
            weeklyOpenStartHour = p[OPEN_START] ?: 18,
            weeklyOpenEndHour = p[OPEN_END] ?: 20,
            lastOpenIsoWeek = p[LAST_OPEN_WEEK] ?: 0,
            deviceLockedToday = p[LOCKED_TODAY] ?: false,
            lastResetDayOfYear = p[LAST_RESET_DOY] ?: 0,
        )
    }

    /** Reads the current settings once (for use off the main flow). */
    suspend fun current(): AppSettings = settings.first()

    suspend fun setDailyLimit(minutes: Int) = context.settingsDataStore.edit {
        it[DAILY_LIMIT] = minutes.coerceIn(
            AppSettings.MIN_DAILY_LIMIT_MIN, AppSettings.MAX_DAILY_LIMIT_MIN,
        )
    }

    suspend fun setFullLockMinutes(minutes: Int) = context.settingsDataStore.edit {
        it[FULL_LOCK] = minutes.coerceAtMost(AppSettings.MAX_FULL_LOCK_MIN)
    }

    suspend fun setFullLockCountsAllApps(value: Boolean) = context.settingsDataStore.edit {
        it[COUNT_ALL] = value
    }

    suspend fun setAutoResetAtMidnight(value: Boolean) = context.settingsDataStore.edit {
        it[AUTO_RESET] = value
    }

    suspend fun setWeeklyWindow(dayOfWeek: Int, startHour: Int, endHour: Int) =
        context.settingsDataStore.edit {
            it[OPEN_DOW] = dayOfWeek.coerceIn(1, 7)
            it[OPEN_START] = startHour.coerceIn(0, 23)
            it[OPEN_END] = endHour.coerceIn(0, 23)
        }

    suspend fun setLastOpenIsoWeek(week: Int) = context.settingsDataStore.edit {
        it[LAST_OPEN_WEEK] = week
    }

    suspend fun setDeviceLockedToday(locked: Boolean) = context.settingsDataStore.edit {
        it[LOCKED_TODAY] = locked
    }

    suspend fun setLastResetDayOfYear(doy: Int) = context.settingsDataStore.edit {
        it[LAST_RESET_DOY] = doy
    }

    companion object {
        private val DAILY_LIMIT = intPreferencesKey("daily_limit_min")
        private val FULL_LOCK = intPreferencesKey("full_lock_min")
        private val COUNT_ALL = booleanPreferencesKey("full_lock_counts_all")
        private val AUTO_RESET = booleanPreferencesKey("auto_reset_midnight")
        private val OPEN_DOW = intPreferencesKey("open_dow")
        private val OPEN_START = intPreferencesKey("open_start_hour")
        private val OPEN_END = intPreferencesKey("open_end_hour")
        private val LAST_OPEN_WEEK = intPreferencesKey("last_open_iso_week")
        private val LOCKED_TODAY = booleanPreferencesKey("device_locked_today")
        private val LAST_RESET_DOY = intPreferencesKey("last_reset_doy")

        // Unused keys kept for forward-compat / clarity.
        @Suppress("unused")
        private val RESERVED_LONG = longPreferencesKey("reserved")
        @Suppress("unused")
        private val RESERVED_STR = stringPreferencesKey("reserved_str")
    }
}
