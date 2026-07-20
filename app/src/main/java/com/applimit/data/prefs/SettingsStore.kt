package com.applimit.data.prefs

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

val Context.settingsDataStore by preferencesDataStore(name = "applimit_settings")

/**
 * Parent-configurable values. The two headline limits:
 *  - generalLimitMinutes ("Allgemeines Limit"): pool for LIMIT + STANDARD apps.
 *  - globalLimitMinutes  ("Globales Limit"): total screen time; LIMIT + STANDARD
 *    plus PLUS apps whose plusCountsToGlobal flag is set.
 * Hard caps live here as code constants.
 */
data class AppSettings(
    /** Master switch. While false nothing is ever blocked (setup mode). */
    val protectionEnabled: Boolean = false,

    val generalLimitMinutes: Int = DEFAULT_GENERAL_MIN,
    val globalLimitMinutes: Int = DEFAULT_GLOBAL_MIN,

    // Ruhezeit / daily usage window.
    val quietTimeEnabled: Boolean = true,
    val usageWindowStartHour: Int = 7,
    val usageWindowEndHour: Int = 20,

    // Optional once-per-week open gate for the child portal.
    val weeklyLockEnabled: Boolean = false,
    val weeklyOpenDayOfWeek: Int = 7,
    val weeklyOpenStartHour: Int = 18,
    val weeklyOpenEndHour: Int = 20,
    val lastOpenIsoWeek: Int = 0,

    /**
     * "Aus-Button": epoch millis until which all time limits + Ruhezeit are
     * paused (default until 23:00 today). 0 = not paused. Automatically expires.
     */
    val limitsPausedUntilMillis: Long = 0,
) {
    fun isInsideUsageWindow(minuteOfDay: Int): Boolean {
        val start = usageWindowStartHour * 60
        val end = usageWindowEndHour * 60
        return if (start <= end) minuteOfDay in start until end
        else minuteOfDay >= start || minuteOfDay < end
    }

    companion object {
        const val DEFAULT_GENERAL_MIN = 60
        const val DEFAULT_GLOBAL_MIN = 120
        const val MAX_LIMIT_MIN = 600        // hard cap for either headline limit
        const val MIN_LIMIT_MIN = 5
    }
}

class SettingsStore(private val context: Context) {

    val settings: Flow<AppSettings> = context.settingsDataStore.data.map { p ->
        AppSettings(
            protectionEnabled = p[PROTECTION_ON] ?: false,
            generalLimitMinutes = (p[GENERAL_LIMIT] ?: AppSettings.DEFAULT_GENERAL_MIN)
                .coerceIn(AppSettings.MIN_LIMIT_MIN, AppSettings.MAX_LIMIT_MIN),
            globalLimitMinutes = (p[GLOBAL_LIMIT] ?: AppSettings.DEFAULT_GLOBAL_MIN)
                .coerceIn(AppSettings.MIN_LIMIT_MIN, AppSettings.MAX_LIMIT_MIN),
            quietTimeEnabled = p[QUIET_ON] ?: true,
            usageWindowStartHour = p[WINDOW_START] ?: 7,
            usageWindowEndHour = p[WINDOW_END] ?: 20,
            weeklyLockEnabled = p[WEEKLY_ON] ?: false,
            weeklyOpenDayOfWeek = p[OPEN_DOW] ?: 7,
            weeklyOpenStartHour = p[OPEN_START] ?: 18,
            weeklyOpenEndHour = p[OPEN_END] ?: 20,
            lastOpenIsoWeek = p[LAST_OPEN_WEEK] ?: 0,
            limitsPausedUntilMillis = p[PAUSED_UNTIL] ?: 0L,
        )
    }

    suspend fun current(): AppSettings = settings.first()

    suspend fun setProtectionEnabled(value: Boolean) = context.settingsDataStore.edit {
        it[PROTECTION_ON] = value
    }

    suspend fun setGeneralLimit(minutes: Int) = context.settingsDataStore.edit {
        it[GENERAL_LIMIT] = minutes.coerceIn(AppSettings.MIN_LIMIT_MIN, AppSettings.MAX_LIMIT_MIN)
    }

    suspend fun setGlobalLimit(minutes: Int) = context.settingsDataStore.edit {
        it[GLOBAL_LIMIT] = minutes.coerceIn(AppSettings.MIN_LIMIT_MIN, AppSettings.MAX_LIMIT_MIN)
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

    suspend fun setLimitsPausedUntil(epochMillis: Long) = context.settingsDataStore.edit {
        it[PAUSED_UNTIL] = epochMillis
    }

    companion object {
        private val PROTECTION_ON = booleanPreferencesKey("protection_enabled")
        private val GENERAL_LIMIT = intPreferencesKey("general_limit_min")
        private val GLOBAL_LIMIT = intPreferencesKey("global_limit_min")
        private val QUIET_ON = booleanPreferencesKey("quiet_time_enabled")
        private val WINDOW_START = intPreferencesKey("usage_window_start")
        private val WINDOW_END = intPreferencesKey("usage_window_end")
        private val WEEKLY_ON = booleanPreferencesKey("weekly_lock_enabled")
        private val OPEN_DOW = intPreferencesKey("open_dow")
        private val OPEN_START = intPreferencesKey("open_start_hour")
        private val OPEN_END = intPreferencesKey("open_end_hour")
        private val LAST_OPEN_WEEK = intPreferencesKey("last_open_iso_week")
        private val PAUSED_UNTIL = longPreferencesKey("limits_paused_until")
    }
}
