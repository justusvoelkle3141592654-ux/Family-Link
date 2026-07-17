package com.applimit.domain

import android.app.AppOpsManager
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/**
 * Thin wrapper over [UsageStatsManager]. We query authoritative per-package
 * foreground time since local midnight, so the numbers survive the service
 * being killed and restarted (unlike an in-memory tick counter).
 *
 * Requires the PACKAGE_USAGE_STATS special access, which the parent grants
 * manually in Settings during onboarding (Prompt Punkt 5).
 */
class UsageStatsReader(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** True once the parent has granted "Usage access" for this app. */
    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** Foreground milliseconds per package since local midnight today. */
    fun foregroundMillisSinceMidnight(): Map<String, Long> {
        val start = startOfTodayMillis()
        val now = System.currentTimeMillis()
        val stats = usageStatsManager.queryUsageStats(
            UsageStatsManager.INTERVAL_DAILY, start, now,
        ) ?: return emptyMap()

        val result = HashMap<String, Long>()
        for (s in stats) {
            if (s.totalTimeInForeground <= 0) continue
            // Multiple buckets can be returned per package; keep the max.
            val prev = result[s.packageName] ?: 0L
            if (s.totalTimeInForeground > prev) {
                result[s.packageName] = s.totalTimeInForeground
            }
        }
        return result
    }

    private fun startOfTodayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }
}
