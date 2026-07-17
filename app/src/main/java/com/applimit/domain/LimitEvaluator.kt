package com.applimit.domain

import com.applimit.data.db.AppCategory
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings

/** What the enforcement service should do right now. */
enum class EnforcementAction {
    ALLOW,          // nothing to do
    BLOCK_APP,      // show blocking overlay over the current app
    LOCK_DEVICE,    // trigger the full-device lock
}

data class LimitDecision(
    val action: EnforcementAction,
    val reason: String,
    val limitedUsedMinutes: Int,
    val dailyLimitMinutes: Int,
    val totalUsedMinutes: Int,
    val fullLockMinutes: Int,
)

/**
 * Pure decision logic — no Android dependencies, so it is unit-testable.
 *
 * Rules (Prompt Punkt 3):
 *  - Sum of "LIMITED" foreground time is compared to the daily limit.
 *  - The full-device-lock budget is either total time (all apps) or limited-app
 *    time only, depending on [AppSettings.fullLockCountsAllApps].
 *  - BLOCKED apps are always blocked; PLUS apps are always allowed (unless the
 *    device-wide full lock has tripped).
 */
object LimitEvaluator {

    fun evaluate(
        foregroundPackage: String?,
        usageMillisByPackage: Map<String, Long>,
        managedApps: List<ManagedApp>,
        settings: AppSettings,
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

        // 1) Device-wide full lock already tripped, or the budget reached it.
        if (settings.deviceLockedToday || budgetMin >= settings.fullLockMinutes) {
            return LimitDecision(
                action = EnforcementAction.LOCK_DEVICE,
                reason = "Gesamt-Nutzungsgrenze erreicht",
                limitedUsedMinutes = limitedMin,
                dailyLimitMinutes = settings.dailyLimitMinutes,
                totalUsedMinutes = totalMin,
                fullLockMinutes = settings.fullLockMinutes,
            )
        }

        val category = foregroundPackage?.let { categoryByPackage[it] }

        // 2) Explicitly blocked app.
        if (category == AppCategory.BLOCKED) {
            return decision(EnforcementAction.BLOCK_APP, "App ist gesperrt", limitedMin, totalMin, settings)
        }

        // 3) Limited app that has hit the shared daily limit.
        if (category == AppCategory.LIMITED && limitedMin >= settings.dailyLimitMinutes) {
            return decision(EnforcementAction.BLOCK_APP, "Tageslimit erreicht", limitedMin, totalMin, settings)
        }

        // 4) Everything else is allowed (PLUS apps, unmanaged apps, our own UI).
        return decision(EnforcementAction.ALLOW, "OK", limitedMin, totalMin, settings)
    }

    private fun decision(
        action: EnforcementAction,
        reason: String,
        limitedMin: Int,
        totalMin: Int,
        settings: AppSettings,
    ) = LimitDecision(
        action = action,
        reason = reason,
        limitedUsedMinutes = limitedMin,
        dailyLimitMinutes = settings.dailyLimitMinutes,
        totalUsedMinutes = totalMin,
        fullLockMinutes = settings.fullLockMinutes,
    )
}
