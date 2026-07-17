package com.applimit.domain

import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings

/** What the enforcement service should do right now. */
enum class EnforcementAction {
    ALLOW,          // nothing to do
    BLOCK_APP,      // show blocking overlay over the current app
    LOCK_DEVICE,    // show the full-device lock screen
}

data class LimitDecision(
    val action: EnforcementAction,
    val reason: String,
    val limitedUsedMinutes: Int,
    val dailyLimitMinutes: Int,
    val totalUsedMinutes: Int,
    val fullLockMinutes: Int,
    /**
     * Only meaningful for LOCK_DEVICE. true → the lock is a hard daily cap and
     * should be persisted (survives restarts until midnight reset). false → a
     * transient lock (Ruhezeit) that clears itself once the window reopens.
     */
    val persistentLock: Boolean = false,
)

/**
 * Pure, Android-free decision logic (unit-testable).
 *
 * Rule order (first match wins):
 *  0. Protection master switch OFF        → ALLOW everything (setup mode).
 *  1. Phone/emergency dialer              → ALLOW always (even while locked).
 *  2. Hard daily budget reached / persisted→ LOCK_DEVICE (persistent).
 *  3. Outside the usage window (Ruhezeit) → LOCK_DEVICE (transient).
 *  4. Android Settings app                → BLOCK_APP (anti-tamper).
 *  5. BLOCKED category app                → BLOCK_APP.
 *  6. LIMITED app past the daily limit    → BLOCK_APP.
 *  7. Otherwise                           → ALLOW.
 *
 * The caller (Enforcer) additionally always allows this app's own package, so
 * the youth portal stays reachable.
 */
object LimitEvaluator {

    /** Substrings identifying the phone/dialer so emergency calls always work. */
    private val PHONE_HINTS = listOf(".dialer", ".phone", ".incallui", "com.android.server.telecom")

    /**
     * Settings / permission-manager packages we block once protection is on, so
     * the child can't disable our overlay or accessibility service. Covers stock
     * Android plus the common OEM security/permission apps.
     */
    private val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.android.settings.intelligence",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.miui.securitycenter",            // Xiaomi
        "com.coloros.safecenter",             // Oppo/Realme (older)
        "com.oplus.safecenter",               // Oppo/Realme (newer)
        "com.oppo.safe",
        "com.samsung.android.settings",       // Samsung
        "com.samsung.android.sm",             // Samsung device care
    )

    fun evaluate(
        foregroundPackage: String?,
        usageMillisByPackage: Map<String, Long>,
        managedApps: List<ManagedApp>,
        settings: AppSettings,
        nowMinuteOfDay: Int,
    ): LimitDecision {
        val categoryByPackage = managedApps.associate { it.packageName to it.category }

        var limitedMs = 0L
        var totalMs = 0L
        for ((pkg, ms) in usageMillisByPackage) {
            totalMs += ms
            if (categoryByPackage[pkg] == AppCategory.LIMITED) limitedMs += ms
        }
        val limitedMin = (limitedMs / 60_000L).toInt()
        val totalMin = (totalMs / 60_000L).toInt()
        val budgetMin = if (settings.fullLockCountsAllApps) totalMin else limitedMin

        // 0) Setup mode / protection off → never block anything.
        if (!settings.protectionEnabled) {
            return decision(EnforcementAction.ALLOW, "Schutz inaktiv", limitedMin, totalMin, settings)
        }

        // 1) Phone/emergency is always allowed, even during a full lock.
        if (isPhone(foregroundPackage)) {
            return decision(EnforcementAction.ALLOW, "Telefon erlaubt", limitedMin, totalMin, settings)
        }

        // 2) Hard daily budget reached (or already tripped today).
        if (settings.deviceLockedToday || budgetMin >= settings.fullLockMinutes) {
            return decision(
                EnforcementAction.LOCK_DEVICE, "Gesamt-Nutzungsgrenze erreicht",
                limitedMin, totalMin, settings, persistentLock = true,
            )
        }

        // 3) Ruhezeit: outside the allowed usage window → lock (transient).
        if (settings.quietTimeEnabled && !settings.isInsideUsageWindow(nowMinuteOfDay)) {
            return decision(
                EnforcementAction.LOCK_DEVICE, "Ruhezeit",
                limitedMin, totalMin, settings, persistentLock = false,
            )
        }

        val category = foregroundPackage?.let { categoryByPackage[it] }

        // 4) Block the Android settings app so the child can't disable us.
        if (foregroundPackage in SETTINGS_PACKAGES) {
            return decision(EnforcementAction.BLOCK_APP, "Einstellungen gesperrt", limitedMin, totalMin, settings)
        }

        // 5) Explicitly blocked app.
        if (category == AppCategory.BLOCKED) {
            return decision(EnforcementAction.BLOCK_APP, "App ist gesperrt", limitedMin, totalMin, settings)
        }

        // 6) Limited app that has hit the shared daily limit.
        if (category == AppCategory.LIMITED && limitedMin >= settings.dailyLimitMinutes) {
            return decision(EnforcementAction.BLOCK_APP, "Tageslimit erreicht", limitedMin, totalMin, settings)
        }

        // 7) Everything else is allowed.
        return decision(EnforcementAction.ALLOW, "OK", limitedMin, totalMin, settings)
    }

    private fun isPhone(pkg: String?): Boolean {
        if (pkg == null) return false
        return PHONE_HINTS.any { pkg.contains(it, ignoreCase = true) }
    }

    private fun decision(
        action: EnforcementAction,
        reason: String,
        limitedMin: Int,
        totalMin: Int,
        settings: AppSettings,
        persistentLock: Boolean = false,
    ) = LimitDecision(
        action = action,
        reason = reason,
        limitedUsedMinutes = limitedMin,
        dailyLimitMinutes = settings.dailyLimitMinutes,
        totalUsedMinutes = totalMin,
        fullLockMinutes = settings.fullLockMinutes,
        persistentLock = persistentLock,
    )
}
