package com.applimit.domain

import com.applimit.data.prefs.AppSettings
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.temporal.WeekFields
import java.util.Locale

/**
 * Once-per-week open logic (Prompt Punkt 1).
 *
 * The child may open the app only inside a configurable weekly window
 * (e.g. Sunday 18:00–20:00) and only once per calendar week. After that it
 * stays locked until the next week — even with the correct child PIN.
 *
 * The PARENT PIN always bypasses this (emergency reset, Punkt 6).
 */
object WeeklyAccess {

    /** Returns yyyyWW-style key identifying the ISO week of [now]. */
    fun isoWeekKey(now: LocalDateTime): Int {
        val wf = WeekFields.of(Locale.getDefault())
        val week = now.get(wf.weekOfWeekBasedYear())
        val year = now.get(wf.weekBasedYear())
        return year * 100 + week
    }

    /** True if [now] falls inside the configured day + hour window. */
    fun isWithinWindow(settings: AppSettings, now: LocalDateTime): Boolean {
        val dow: DayOfWeek = now.dayOfWeek // MONDAY(1)..SUNDAY(7)
        if (dow.value != settings.weeklyOpenDayOfWeek) return false
        val hour = now.hour
        return hour >= settings.weeklyOpenStartHour && hour < settings.weeklyOpenEndHour
    }

    /**
     * Whether the child is allowed to open the app right now: inside the window
     * and not already opened during this ISO week.
     */
    fun canChildOpen(settings: AppSettings, now: LocalDateTime): Boolean {
        if (!isWithinWindow(settings, now)) return false
        return settings.lastOpenIsoWeek != isoWeekKey(now)
    }

    /** Human-readable reason shown on the lock screen when access is denied. */
    fun lockReason(settings: AppSettings, now: LocalDateTime): String {
        return if (!isWithinWindow(settings, now)) {
            val dayName = germanDay(settings.weeklyOpenDayOfWeek)
            "Die App ist nur $dayName von ${settings.weeklyOpenStartHour}:00 bis " +
                "${settings.weeklyOpenEndHour}:00 Uhr freigeschaltet."
        } else {
            "Die App wurde diese Woche bereits geöffnet und ist bis nächste Woche gesperrt."
        }
    }

    private fun germanDay(dow: Int): String = when (dow) {
        1 -> "montags"; 2 -> "dienstags"; 3 -> "mittwochs"; 4 -> "donnerstags"
        5 -> "freitags"; 6 -> "samstags"; else -> "sonntags"
    }
}
