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

    @Test
    fun `limited app under limit is allowed`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = youtube.packageName,
            usageMillisByPackage = mapOf(youtube.packageName to min(30)),
            managedApps = apps,
            settings = AppSettings(dailyLimitMinutes = 60, fullLockMinutes = 120),
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
        assertEquals(30, d.limitedUsedMinutes)
    }

    @Test
    fun `limited app at daily limit is blocked`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = youtube.packageName,
            usageMillisByPackage = mapOf(youtube.packageName to min(45), games.packageName to min(20)),
            managedApps = apps,
            settings = AppSettings(dailyLimitMinutes = 60, fullLockMinutes = 300),
        )
        // 45 + 20 = 65 >= 60
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
    }

    @Test
    fun `plus app does not count against daily limit`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = school.packageName,
            usageMillisByPackage = mapOf(school.packageName to min(200)),
            managedApps = apps,
            settings = AppSettings(dailyLimitMinutes = 60, fullLockMinutes = 300, fullLockCountsAllApps = false),
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
        assertEquals(0, d.limitedUsedMinutes)
    }

    @Test
    fun `blocked app is always blocked`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = blocked.packageName,
            usageMillisByPackage = emptyMap(),
            managedApps = apps,
            settings = AppSettings(),
        )
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
    }

    @Test
    fun `full lock trips when all apps counted`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = school.packageName,
            usageMillisByPackage = mapOf(school.packageName to min(90), youtube.packageName to min(40)),
            managedApps = apps,
            settings = AppSettings(fullLockMinutes = 120, fullLockCountsAllApps = true),
        )
        // 90 + 40 = 130 >= 120
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
    }

    @Test
    fun `full lock does not trip on plus time when only limited counted`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = school.packageName,
            usageMillisByPackage = mapOf(school.packageName to min(200)),
            managedApps = apps,
            settings = AppSettings(fullLockMinutes = 120, fullLockCountsAllApps = false),
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test
    fun `persisted device lock keeps device locked`() {
        val d = LimitEvaluator.evaluate(
            foregroundPackage = school.packageName,
            usageMillisByPackage = emptyMap(),
            managedApps = apps,
            settings = AppSettings(deviceLockedToday = true),
        )
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
    }
}
