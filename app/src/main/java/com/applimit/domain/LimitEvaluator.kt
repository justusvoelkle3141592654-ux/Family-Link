package com.applimit.domain

import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings

enum class EnforcementAction { ALLOW, BLOCK_APP, LOCK_DEVICE }

/**
 * Result of one evaluation. All usage/limit values are in SECONDS so the
 * enforcement is second-accurate and the parent overview can show exact figures.
 */
data class LimitDecision(
    val action: EnforcementAction,
    val reason: String,
    val generalUsedSec: Long,
    val generalLimitSec: Long,
    val globalUsedSec: Long,
    val globalLimitSec: Long,
    /** Only meaningful when the foreground app is a LIMIT app. */
    val individualUsedSec: Long = 0,
    val individualLimitSec: Long = 0,
)

/**
 * Pure, Android-free decision logic for the two-tier limit system.
 *
 * Two pools (see [AppSettings]):
 *   - GENERAL: LIMIT + STANDARD apps.
 *   - GLOBAL:  LIMIT + STANDARD + PLUS apps whose plusCountsToGlobal is set.
 *
 * Overlay rules (first match wins), per foreground category:
 *   BLOCKED  → always blocked.
 *   PLUS     → never blocked (even if the global pool is exhausted).
 *   LIMIT    → blocked when individual OR general OR global limit reached.
 *   STANDARD → blocked when general OR global limit reached.
 *
 * Ordering also covers: protection off, phone/emergency, Ruhezeit, Settings app.
 */
object LimitEvaluator {

    private val PHONE_HINTS = listOf(".dialer", ".phone", ".incallui", "com.android.server.telecom")

    private val SETTINGS_PACKAGES = setOf(
        "com.android.settings",
        "com.android.settings.intelligence",
        "com.android.permissioncontroller",
        "com.google.android.permissioncontroller",
        "com.miui.securitycenter",
        "com.coloros.safecenter",
        "com.oplus.safecenter",
        "com.oppo.safe",
        "com.samsung.android.settings",
        "com.samsung.android.sm",
    )

    fun evaluate(
        foregroundPackage: String?,
        usageMillisByPackage: Map<String, Long>,
        managedApps: List<ManagedApp>,
        settings: AppSettings,
        nowMinuteOfDay: Int,
        /** Real user apps that count as STANDARD by default when not categorised. */
        defaultStandardPackages: Set<String> = emptySet(),
        /** True while the parent's "Aus-Button" pause is active. */
        limitsPaused: Boolean = false,
    ): LimitDecision {
        val appByPackage = managedApps.associateBy { it.packageName }

        // Effective category: explicit assignment, else STANDARD for real user
        // apps (so uncategorised apps still count towards the limits), else null.
        fun categoryOf(pkg: String): AppCategory? =
            appByPackage[pkg]?.category
                ?: if (pkg in defaultStandardPackages) AppCategory.STANDARD else null

        var generalMs = 0L
        var globalMs = 0L
        for ((pkg, ms) in usageMillisByPackage) {
            when (categoryOf(pkg)) {
                AppCategory.LIMIT, AppCategory.STANDARD -> { generalMs += ms; globalMs += ms }
                AppCategory.PLUS -> if (appByPackage[pkg]?.plusCountsToGlobal == true) globalMs += ms
                AppCategory.BLOCKED, null -> { /* counts to nothing */ }
            }
        }

        val generalLimitMs = settings.generalLimitMinutes * 60_000L
        val globalLimitMs = settings.globalLimitMinutes * 60_000L

        val fgManaged = foregroundPackage?.let { appByPackage[it] }
        val fgCat = foregroundPackage?.let { categoryOf(it) }
        val fgUsedMs = foregroundPackage?.let { usageMillisByPackage[it] } ?: 0L
        val indivLimitMs =
            if (fgManaged?.category == AppCategory.LIMIT) fgManaged.individualLimitMinutes * 60_000L else 0L

        fun build(action: EnforcementAction, reason: String) = LimitDecision(
            action = action,
            reason = reason,
            generalUsedSec = generalMs / 1000,
            generalLimitSec = generalLimitMs / 1000,
            globalUsedSec = globalMs / 1000,
            globalLimitSec = globalLimitMs / 1000,
            individualUsedSec = if (indivLimitMs > 0) fgUsedMs / 1000 else 0,
            individualLimitSec = indivLimitMs / 1000,
        )

        // 0) Setup mode.
        if (!settings.protectionEnabled) return build(EnforcementAction.ALLOW, "Schutz inaktiv")

        // 1) Phone/emergency always allowed.
        if (isPhone(foregroundPackage)) return build(EnforcementAction.ALLOW, "Telefon erlaubt")

        // 2) Settings/permission apps are always bounced (anti-tamper).
        if (foregroundPackage in SETTINGS_PACKAGES) {
            return build(EnforcementAction.BLOCK_APP, "Einstellungen gesperrt")
        }

        // 3) Explicitly blocked apps stay blocked, even when paused.
        if (fgCat == AppCategory.BLOCKED) {
            return build(EnforcementAction.BLOCK_APP, "App ist gesperrt")
        }

        // 4) "Aus-Button": all time limits + Ruhezeit paused.
        if (limitsPaused) return build(EnforcementAction.ALLOW, "Limits pausiert")

        // 5) Ruhezeit (outside the usage window) → device lock.
        if (settings.quietTimeEnabled && !settings.isInsideUsageWindow(nowMinuteOfDay)) {
            return build(EnforcementAction.LOCK_DEVICE, "Ruhezeit")
        }

        // 6) Category-specific limit rules.
        return when (fgCat) {
            null -> build(EnforcementAction.ALLOW, "Nicht verwaltet")
            AppCategory.PLUS -> build(EnforcementAction.ALLOW, "Zugelassen Plus")
            AppCategory.BLOCKED -> build(EnforcementAction.BLOCK_APP, "App ist gesperrt")
            AppCategory.LIMIT -> when {
                fgUsedMs >= indivLimitMs -> build(EnforcementAction.BLOCK_APP, "Individuelles App-Limit erreicht")
                generalMs >= generalLimitMs -> build(EnforcementAction.LOCK_DEVICE, "Allgemeines Limit erreicht")
                globalMs >= globalLimitMs -> build(EnforcementAction.LOCK_DEVICE, "Globales Limit erreicht")
                else -> build(EnforcementAction.ALLOW, "OK")
            }
            AppCategory.STANDARD -> when {
                generalMs >= generalLimitMs -> build(EnforcementAction.LOCK_DEVICE, "Allgemeines Limit erreicht")
                globalMs >= globalLimitMs -> build(EnforcementAction.LOCK_DEVICE, "Globales Limit erreicht")
                else -> build(EnforcementAction.ALLOW, "OK")
            }
        }
    }

    private fun isPhone(pkg: String?): Boolean {
        if (pkg == null) return false
        return PHONE_HINTS.any { pkg.contains(it, ignoreCase = true) }
    }
}
