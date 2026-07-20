package com.applimit.data.repository

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.applimit.data.db.AppCategory
import com.applimit.data.db.AppLimitDatabase
import com.applimit.data.db.ManagedApp
import com.applimit.data.prefs.AppSettings
import com.applimit.data.prefs.SecurePinStore
import com.applimit.data.prefs.SettingsStore
import com.applimit.domain.LimitDecision
import com.applimit.domain.LimitEvaluator
import com.applimit.domain.UsageStatsReader
import com.applimit.domain.WeeklyAccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

/** Single access point for the rest of the app. */
class AppLimitRepository private constructor(
    private val appContext: Context,
) {
    private val db = AppLimitDatabase.get(appContext)
    private val dao = db.managedAppDao()
    val settingsStore = SettingsStore(appContext)
    val pinStore = SecurePinStore(appContext)
    val usageReader = UsageStatsReader(appContext)

    val managedApps: Flow<List<ManagedApp>> = dao.observeAll()
    val settings: Flow<AppSettings> = settingsStore.settings

    suspend fun setCategory(
        pkg: String,
        name: String,
        category: AppCategory,
        individualLimitMinutes: Int = 30,
        plusCountsToGlobal: Boolean = false,
    ) = dao.upsert(ManagedApp(pkg, name, category, individualLimitMinutes, plusCountsToGlobal))

    suspend fun clearCategory(pkg: String) = dao.delete(pkg)

    /** Lists user-visible installed apps (excludes this app itself). */
    suspend fun installedApps(): List<InstalledApp> = withContext(Dispatchers.IO) {
        val pm = appContext.packageManager
        val flags = PackageManager.GET_META_DATA
        pm.getInstalledApplications(flags)
            .asSequence()
            .filter { it.packageName != appContext.packageName }
            .filter { isLaunchable(pm, it) }
            .map { InstalledApp(it.packageName, pm.getApplicationLabel(it).toString()) }
            .sortedBy { it.appName.lowercase() }
            .toList()
    }

    private fun isLaunchable(pm: PackageManager, info: ApplicationInfo): Boolean {
        // Show launchable apps plus updated system apps; hide pure system services.
        val isSystem = (info.flags and ApplicationInfo.FLAG_SYSTEM) != 0
        val hasLauncher = pm.getLaunchIntentForPackage(info.packageName) != null
        return hasLauncher || !isSystem
    }

    /** Evaluates the current enforcement decision for [foregroundPackage]. */
    suspend fun evaluate(foregroundPackage: String?): LimitDecision =
        withContext(Dispatchers.IO) {
            val apps = dao.getAll()
            val settings = settingsStore.current()
            val usage = usageReader.foregroundMillisSinceMidnight()
            val now = LocalDateTime.now()
            val minuteOfDay = now.hour * 60 + now.minute
            LimitEvaluator.evaluate(foregroundPackage, usage, apps, settings, minuteOfDay)
        }

    /**
     * Transparent usage breakdown for the parent overview: the two headline
     * pools plus every managed app's own consumed time.
     */
    suspend fun usageOverview(): UsageOverview = withContext(Dispatchers.IO) {
        val apps = dao.getAll()
        val settings = settingsStore.current()
        val usage = usageReader.foregroundMillisSinceMidnight()
        val byPkg = apps.associateBy { it.packageName }

        var generalMs = 0L
        var globalMs = 0L
        for ((pkg, ms) in usage) {
            val app = byPkg[pkg] ?: continue
            when (app.category) {
                AppCategory.LIMIT, AppCategory.STANDARD -> { generalMs += ms; globalMs += ms }
                AppCategory.PLUS -> if (app.plusCountsToGlobal) globalMs += ms
                AppCategory.BLOCKED -> {}
            }
        }
        val appUsages = apps.map { app ->
            AppUsage(
                packageName = app.packageName,
                appName = app.appName,
                category = app.category,
                usedSeconds = (usage[app.packageName] ?: 0L) / 1000,
                individualLimitMinutes = if (app.category == AppCategory.LIMIT) app.individualLimitMinutes else null,
            )
        }.sortedByDescending { it.usedSeconds }

        UsageOverview(
            generalUsedSeconds = generalMs / 1000,
            generalLimitSeconds = settings.generalLimitMinutes * 60L,
            globalUsedSeconds = globalMs / 1000,
            globalLimitSeconds = settings.globalLimitMinutes * 60L,
            apps = appUsages,
        )
    }

    // ----- Weekly open gating -----

    suspend fun canChildOpenNow(): Boolean {
        val settings = settingsStore.current()
        // The youth portal is always reachable unless the parent explicitly
        // turned on the once-per-week lock.
        if (!settings.weeklyLockEnabled) return true
        return WeeklyAccess.canChildOpen(settings, LocalDateTime.now())
    }

    suspend fun lockReason(): String {
        val settings = settingsStore.current()
        return WeeklyAccess.lockReason(settings, LocalDateTime.now())
    }

    /** Record that the child opened the app during this week's window. */
    suspend fun markChildOpened() {
        settingsStore.setLastOpenIsoWeek(WeeklyAccess.isoWeekKey(LocalDateTime.now()))
    }

    /** Emergency reset by the parent: reopen this week's portal gate. */
    suspend fun parentEmergencyReset() {
        settingsStore.setLastOpenIsoWeek(0)
    }

    companion object {
        @Volatile private var INSTANCE: AppLimitRepository? = null
        fun get(context: Context): AppLimitRepository =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLimitRepository(context.applicationContext).also { INSTANCE = it }
            }
    }
}

data class InstalledApp(val packageName: String, val appName: String)

data class AppUsage(
    val packageName: String,
    val appName: String,
    val category: AppCategory,
    val usedSeconds: Long,
    val individualLimitMinutes: Int?,
)

data class UsageOverview(
    val generalUsedSeconds: Long,
    val generalLimitSeconds: Long,
    val globalUsedSeconds: Long,
    val globalLimitSeconds: Long,
    val apps: List<AppUsage>,
)
