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

    suspend fun setCategory(pkg: String, name: String, category: AppCategory) =
        dao.upsert(ManagedApp(pkg, name, category))

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
            LimitEvaluator.evaluate(foregroundPackage, usage, apps, settings)
        }

    // ----- Weekly open gating (Punkt 1) -----

    suspend fun canChildOpenNow(): Boolean {
        val settings = settingsStore.current()
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

    /** Emergency reset by the parent: reopen this week + clear the full lock. */
    suspend fun parentEmergencyReset() {
        settingsStore.setLastOpenIsoWeek(0)
        settingsStore.setDeviceLockedToday(false)
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
