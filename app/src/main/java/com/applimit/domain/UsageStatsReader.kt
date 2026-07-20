package com.applimit.domain

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import java.util.Calendar

/**
 * Reads accurate per-app foreground time since local midnight.
 *
 * IMPORTANT: we deliberately do NOT use queryUsageStats(INTERVAL_DAILY), because
 * its aggregated buckets frequently overlap and double-count, which produced the
 * "limit reached at 0 minutes" bug. Instead we replay the raw UsageEvents stream
 * (ACTIVITY_RESUMED → ACTIVITY_PAUSED / MOVE_TO_FOREGROUND → MOVE_TO_BACKGROUND)
 * and sum the real time each package spent in the foreground today. This mirrors
 * how the system itself computes screen time and matches the phone's own usage
 * figures.
 *
 * Requires the PACKAGE_USAGE_STATS special access, granted manually in Settings.
 */
class UsageStatsReader(private val context: Context) {

    private val usageStatsManager =
        context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager

    /** True once the parent has granted "Usage access" for this app. */
    fun hasUsageAccess(): Boolean {
        return try {
            val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
            val mode = appOps.unsafeCheckOpNoThrow(
                AppOpsManager.OPSTR_GET_USAGE_STATS,
                Process.myUid(),
                context.packageName,
            )
            mode == AppOpsManager.MODE_ALLOWED
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Accurate foreground milliseconds per package since local midnight today.
     * Returns an empty map if usage access is missing (so nothing is ever
     * over-counted).
     */
    fun foregroundMillisSinceMidnight(): Map<String, Long> {
        if (!hasUsageAccess()) return emptyMap()

        val start = startOfTodayMillis()
        val now = System.currentTimeMillis()

        val result = HashMap<String, Long>()
        // Tracks the resume timestamp of the currently-foregrounded package.
        var currentPkg: String? = null
        var currentStart = 0L

        try {
            val events = usageStatsManager.queryEvents(start, now)
            val event = UsageEvents.Event()
            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED,
                    UsageEvents.Event.MOVE_TO_FOREGROUND -> {
                        // A new app came to the front; close out the previous one.
                        if (currentPkg != null) {
                            add(result, currentPkg!!, event.timeStamp - currentStart)
                        }
                        currentPkg = event.packageName
                        currentStart = event.timeStamp
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED,
                    UsageEvents.Event.MOVE_TO_BACKGROUND,
                    UsageEvents.Event.ACTIVITY_STOPPED -> {
                        if (event.packageName == currentPkg && currentPkg != null) {
                            add(result, currentPkg!!, event.timeStamp - currentStart)
                            currentPkg = null
                        }
                    }
                }
            }
            // Still-foregrounded app: count up to "now".
            if (currentPkg != null) {
                add(result, currentPkg!!, now - currentStart)
            }
        } catch (_: Exception) {
            return emptyMap()
        }
        return result
    }

    /**
     * Best-effort current foreground package from the last resume event in the
     * recent past. Used as a fallback when the AccessibilityService is quiet, so
     * limit locks and blocking still trigger on the periodic re-check.
     */
    fun currentForegroundPackage(): String? {
        if (!hasUsageAccess()) return null
        val now = System.currentTimeMillis()
        return try {
            val events = usageStatsManager.queryEvents(now - 60_000, now)
            val e = UsageEvents.Event()
            var last: String? = null
            while (events.hasNextEvent()) {
                events.getNextEvent(e)
                if (e.eventType == UsageEvents.Event.ACTIVITY_RESUMED ||
                    e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
                ) {
                    last = e.packageName
                }
            }
            last
        } catch (_: Exception) {
            null
        }
    }

    private fun add(map: HashMap<String, Long>, pkg: String, delta: Long) {
        if (delta <= 0 || delta > MAX_SINGLE_SEGMENT_MS) return // ignore bogus spans
        map[pkg] = (map[pkg] ?: 0L) + delta
    }

    private fun startOfTodayMillis(): Long {
        val c = Calendar.getInstance()
        c.set(Calendar.HOUR_OF_DAY, 0)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    companion object {
        // A single uninterrupted foreground segment longer than this is almost
        // certainly a missed PAUSE event; ignore it so we never over-count.
        private const val MAX_SINGLE_SEGMENT_MS = 6 * 60 * 60 * 1000L // 6h
    }
}
