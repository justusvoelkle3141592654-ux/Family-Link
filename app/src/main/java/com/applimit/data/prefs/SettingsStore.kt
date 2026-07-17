package com.applimit.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "applimit_settings")

/**
 * All parent-configurable values. The hard upper bounds live here as code
 * constants so the UI can never exceed them ("harte Obergrenze im Code").
 */
data class AppSettings(
    /**
     * MASTER SWITCH. While false the app is in "setup mode": nothing is ever
     * blocked or locked. Enforcement only begins once the parent turns this on
     * in the parent area. Default false so first-run setup never locks the phone.
     */
    val protectionEnabled: Boolean = false,

    val dailyLimitMinutes: Int = DEFAULT_DAILY_LIMIT_MIN,
    val fullLockMinutes: Int = DEFAULT_FULL_LOCK_MIN,
    /** true → every foreground app counts towards the full-lock budget. */
    val fullLockCountsAllApps: Boolean = true,
    /** Reset the full lock + counters daily at midnight. */
    val autoResetAtMidnight: Boolean = true,

    // --- Ruhezeit / daily usage window (outside it the device is locked) ---
    val quietTimeEnabled: Boolean = true,
    /** Usage is allowed only between these hours; outside = Ruhezeit. Default 7–20. */
    val usageWindowStartHour: Int = 7,
    val usageWindowEndHour: Int = 20,

    // --- Once-per-week open gate for the child portal (off by default now:
    //     the youth portal should always be reachable with the child PIN) ---
    val weeklyLockEnabled: Boolean = false,
    val weeklyOpenDayOfWeek: Int = 7,
    val weeklyOpenStartHour: Int = 18,
    val weeklyOpenEndHour: Int = 20,
    val lastOpenIsoWeek: Int = 0,

    /** True once the full-device lock has tripped for the current day. */
    val deviceLockedToday: Boolean = false,
    /** yyyyDDD day-of-year marker of the last midnight reset. */
    val lastResetDayOfYear: Int = 0,
) {
    /** True if [minuteOfDay] (0..1439) falls inside the allowed usage window. */
    fun isInsideUsageWindow(minuteOfDay: Int): Boolean {
        val start = usageWindowStartHour * 60
        val end = usageWindowEndHour * 60
        return if (start <= end) {
            minuteOfDay in start until end
        } else {
            // Window wraps past midnight (e.g. 20 → 7).
            minuteOfDay >= start || minuteOfDay < end
        }
    }

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
            protectionEnabled = p[PROTECTION_ON] ?: false,
            dailyLimitMinutes = (p[DAILY_LIMIT] ?: AppSettings.DEFAULT_DAILY_LIMIT_MIN)
                .coerceIn(AppSettings.MIN_DAILY_LIMIT_MIN, AppSettings.MAX_DAILY_LIMIT_MIN),
            fullLockMinutes = (p[FULL_LOCK] ?: AppSettings.DEFAULT_FULL_LOCK_MIN)
                .coerceAtMost(AppSettings.MAX_FULL_LOCK_MIN),
            fullLockCountsAllApps = p[COUNT_ALL] ?: true,
            autoResetAtMidnight = p[AUTO_RESET] ?: true,
            quietTimeEnabled = p[QUIET_ON] ?: true,
            usageWindowStartHour = p[WINDOW_START] ?: 7,
            usageWindowEndHour = p[WINDOW_END] ?: 20,
            weeklyLockEnabled = p[WEEKLY_ON] ?: false,
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

    suspend fun setProtectionEnabled(value: Boolean) = context.settingsDataStore.edit {
        it[PROTECTION_ON] = value
    }

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

    suspend fun setQuietTimeEnabled(value: Boolean) = context.settingsDataStore.edit {
        it[QUIET_ON] = value
    }

    suspend fun setUsageWindow(startHour: Int, endHour: Int) = context.settingsDataStore.edit {
        it[WINDOW_START] = startHour.coerceIn(0, 23)
        it[WINDOW_END] = endHour.coerceIn(1, 24)
    }

    suspend fun setWeeklyLockEnabled(value: Boolean) = context.settingsDataStore.edit {
        it[WEEKLY_ON] = value
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
        private val PROTECTION_ON = booleanPreferencesKey("protection_enabled")
        private val DAILY_LIMIT = intPreferencesKey("daily_limit_min")
        private val FULL_LOCK = intPreferencesKey("full_lock_min")
        private val COUNT_ALL = booleanPreferencesKey("full_lock_counts_all")
        private val AUTO_RESET = booleanPreferencesKey("auto_reset_midnight")
        private val QUIET_ON = booleanPreferencesKey("quiet_time_enabled")
        private val WINDOW_START = intPreferencesKey("usage_window_start")
        private val WINDOW_END = intPreferencesKey("usage_window_end")
        private val WEEKLY_ON = booleanPreferencesKey("weekly_lock_enabled")
        private val OPEN_DOW = intPreferencesKey("open_dow")
        private val OPEN_START = intPreferencesKey("open_start_hour")
        private val OPEN_END = intPreferencesKey("open_end_hour")
        private val LAST_OPEN_WEEK = intPreferencesKey("last_open_iso_week")
        private val LOCKED_TODAY = booleanPreferencesKey("device_locked_today")
        private val LAST_RESET_DOY = intPreferencesKey("last_reset_doy")
    }
}
