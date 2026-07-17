package com.applimit.domain

import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitEvaluatorTest {

    private val youtube = ManagedApp("com.google.youtube", "YouTube", AppCategory.LIMITED)
    private val games = ManagedApp("com.game.app", "Game", AppCategory.LIMITED)
    private val school = ManagedApp("com.school.app", "School", AppCategory.PLUS)
    private val blocked = ManagedApp("com.bad.app", "Bad", AppCategory.BLOCKED)
    private val apps = listOf(youtube, games, school, blocked)

    private fun min(m: Int) = m * 60_000L

    /** Protection on + inside the 7–20 usage window (12:00). */
    private fun base() = AppSettings(protectionEnabled = true)
    private val noon = 12 * 60

    @Test
    fun `protection off allows everything`() {
        val d = LimitEvaluator.evaluate(
            blocked.packageName, mapOf(youtube.packageName to min(999)), apps,
            AppSettings(protectionEnabled = false), noon,
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test
    fun `limited app under limit is allowed`() {
        val d = LimitEvaluator.evaluate(
            youtube.packageName, mapOf(youtube.packageName to min(30)), apps,
            base().copy(dailyLimitMinutes = 60, fullLockMinutes = 120), noon,
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
        assertEquals(30, d.limitedUsedMinutes)
    }

    @Test
    fun `limited app at daily limit is blocked`() {
        val d = LimitEvaluator.evaluate(
            youtube.packageName,
            mapOf(youtube.packageName to min(45), games.packageName to min(20)),
            apps, base().copy(dailyLimitMinutes = 60, fullLockMinutes = 300), noon,
        )
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
    }

    @Test
    fun `plus app does not count against daily limit`() {
        val d = LimitEvaluator.evaluate(
            school.packageName, mapOf(school.packageName to min(200)), apps,
            base().copy(dailyLimitMinutes = 60, fullLockMinutes = 300, fullLockCountsAllApps = false),
            noon,
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
        assertEquals(0, d.limitedUsedMinutes)
    }

    @Test
    fun `blocked app is always blocked`() {
        val d = LimitEvaluator.evaluate(
            blocked.packageName, emptyMap(), apps, base(), noon,
        )
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
    }

    @Test
    fun `full lock trips when all apps counted`() {
        val d = LimitEvaluator.evaluate(
            school.packageName,
            mapOf(school.packageName to min(90), youtube.packageName to min(40)),
            apps, base().copy(fullLockMinutes = 120, fullLockCountsAllApps = true), noon,
        )
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
    }

    @Test
    fun `persisted device lock keeps device locked`() {
        val d = LimitEvaluator.evaluate(
            school.packageName, emptyMap(), apps,
            base().copy(deviceLockedToday = true), noon,
        )
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
    }

    @Test
    fun `outside usage window is Ruhezeit lock`() {
        // 22:00 is outside the 7–20 window.
        val d = LimitEvaluator.evaluate(
            school.packageName, emptyMap(), apps, base(), 22 * 60,
        )
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
        assertEquals("Ruhezeit", d.reason)
        assertEquals(false, d.persistentLock)
    }

    @Test
    fun `phone is allowed even during ruhezeit`() {
        val d = LimitEvaluator.evaluate(
            "com.android.dialer", emptyMap(), apps, base(), 23 * 60,
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test
    fun `settings app is blocked when protection on`() {
        val d = LimitEvaluator.evaluate(
            "com.android.settings", emptyMap(), apps, base(), noon,
        )
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
    }
}
