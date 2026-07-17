package com.applimit.domain

import com.applimit.data.prefs.AppSettings
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime

class WeeklyAccessTest {

    // Sunday window 18:00–20:00.
    private val settings = AppSettings(
        weeklyOpenDayOfWeek = 7,
        weeklyOpenStartHour = 18,
        weeklyOpenEndHour = 20,
    )

    // 2026-07-19 is a Sunday.
    private val sundayInWindow = LocalDateTime.of(2026, 7, 19, 18, 30)
    private val sundayOutOfWindow = LocalDateTime.of(2026, 7, 19, 21, 0)
    private val monday = LocalDateTime.of(2026, 7, 20, 18, 30)

    @Test
    fun `inside window and not opened yet is allowed`() {
        assertTrue(WeeklyAccess.canChildOpen(settings, sundayInWindow))
    }

    @Test
    fun `outside window is denied`() {
        assertFalse(WeeklyAccess.canChildOpen(settings, sundayOutOfWindow))
        assertFalse(WeeklyAccess.canChildOpen(settings, monday))
    }

    @Test
    fun `already opened this week is denied`() {
        val week = WeeklyAccess.isoWeekKey(sundayInWindow)
        val used = settings.copy(lastOpenIsoWeek = week)
        assertFalse(WeeklyAccess.canChildOpen(used, sundayInWindow))
    }
}
