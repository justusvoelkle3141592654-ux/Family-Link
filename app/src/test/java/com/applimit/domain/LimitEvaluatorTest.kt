package com.applimit.domain

import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class LimitEvaluatorTest {

    private val youtube = ManagedApp("com.yt", "YouTube", AppCategory.LIMIT, individualLimitMinutes = 30)
    private val game = ManagedApp("com.game", "Game", AppCategory.STANDARD)
    private val school = ManagedApp("com.school", "School", AppCategory.PLUS, plusCountsToGlobal = false)
    private val music = ManagedApp("com.music", "Music", AppCategory.PLUS, plusCountsToGlobal = true)
    private val bad = ManagedApp("com.bad", "Bad", AppCategory.BLOCKED)
    private val apps = listOf(youtube, game, school, music, bad)

    private fun min(m: Int) = m * 60_000L
    private fun base() = AppSettings(protectionEnabled = true, generalLimitMinutes = 60, globalLimitMinutes = 120)
    private val noon = 12 * 60

    private fun eval(fg: String?, usage: Map<String, Long>, s: AppSettings = base(), now: Int = noon) =
        LimitEvaluator.evaluate(fg, usage, apps, s, now)

    @Test fun `protection off allows everything`() {
        val d = eval(bad.packageName, mapOf(youtube.packageName to min(999)), AppSettings(protectionEnabled = false))
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `standard under general is allowed`() {
        val d = eval(game.packageName, mapOf(game.packageName to min(30)))
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `standard locks device when general limit reached`() {
        val d = eval(game.packageName, mapOf(game.packageName to min(45), youtube.packageName to min(20)))
        // general = 45 + 20 = 65 >= 60 → device lock
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
        assertEquals("Allgemeines Limit erreicht", d.reason)
    }

    @Test fun `limit app blocked by its individual limit first`() {
        val d = eval(youtube.packageName, mapOf(youtube.packageName to min(30)))
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
        assertEquals("Individuelles App-Limit erreicht", d.reason)
    }

    @Test fun `limit app under all limits is allowed`() {
        val d = eval(youtube.packageName, mapOf(youtube.packageName to min(10)))
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `plus app never blocked even if global exhausted`() {
        val s = base().copy(globalLimitMinutes = 60)
        val d = eval(music.packageName, mapOf(music.packageName to min(500)), s)
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `plus counting to global can lock device for a standard app`() {
        val s = base().copy(generalLimitMinutes = 600, globalLimitMinutes = 120)
        // music (PLUS, counts global) 100 + game (STANDARD) 30 → general 30, global 130
        val d = eval(game.packageName, mapOf(music.packageName to min(100), game.packageName to min(30)), s)
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
        assertEquals("Globales Limit erreicht", d.reason)
    }

    @Test fun `plus not counting to global does not block`() {
        val s = base().copy(generalLimitMinutes = 600, globalLimitMinutes = 120)
        val d = eval(game.packageName, mapOf(school.packageName to min(300), game.packageName to min(10)), s)
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `blocked app is always blocked`() {
        val d = eval(bad.packageName, emptyMap())
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
        assertEquals("App ist gesperrt", d.reason)
    }

    @Test fun `outside window is ruhezeit lock`() {
        val d = eval(game.packageName, emptyMap(), now = 22 * 60)
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
        assertEquals("Ruhezeit", d.reason)
    }

    @Test fun `phone allowed during ruhezeit`() {
        val d = eval("com.android.dialer", emptyMap(), now = 23 * 60)
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `settings app blocked when protection on`() {
        val d = eval("com.android.settings", emptyMap())
        assertEquals(EnforcementAction.BLOCK_APP, d.action)
        assertEquals("Einstellungen gesperrt", d.reason)
    }

    @Test fun `unmanaged app is allowed`() {
        val d = eval("com.random.unmanaged", emptyMap())
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `uncategorised app counts to the limit by default`() {
        // "com.new" is not managed → counts towards the pools automatically.
        val d = LimitEvaluator.evaluate("com.new", mapOf("com.new" to min(70)), apps, base(), noon)
        // 70 >= general 60 → device lock, regardless of foreground category
        assertEquals(EnforcementAction.LOCK_DEVICE, d.action)
        assertEquals("Allgemeines Limit erreicht", d.reason)
    }

    @Test fun `excluded package does not count`() {
        val d = LimitEvaluator.evaluate(
            "com.new", mapOf("com.launcher" to min(200)), apps, base(), noon,
            excludedPackages = setOf("com.launcher"),
        )
        assertEquals(EnforcementAction.ALLOW, d.action)
    }

    @Test fun `pause disables limits and ruhezeit but not blocked apps`() {
        val overGeneral = mapOf(game.packageName to min(90))
        // Paused → standard app allowed despite being over the limit.
        val d1 = LimitEvaluator.evaluate(game.packageName, overGeneral, apps, base(), noon, limitsPaused = true)
        assertEquals(EnforcementAction.ALLOW, d1.action)
        // Paused during Ruhezeit → still allowed.
        val d2 = LimitEvaluator.evaluate(game.packageName, emptyMap(), apps, base(), 23 * 60, limitsPaused = true)
        assertEquals(EnforcementAction.ALLOW, d2.action)
        // But a blocked app stays blocked even when paused.
        val d3 = LimitEvaluator.evaluate(bad.packageName, emptyMap(), apps, base(), noon, limitsPaused = true)
        assertEquals(EnforcementAction.BLOCK_APP, d3.action)
    }
}
