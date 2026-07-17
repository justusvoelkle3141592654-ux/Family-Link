package com.applimit.service

import android.content.Context
import android.util.Log
import com.applimit.data.repository.AppLimitRepository
import com.applimit.domain.EnforcementAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Shared enforcement brain used by both the AccessibilityService (event-driven)
 * and the EnforcementService (time-driven). Everything here is wrapped in
 * try/catch so a single failure can never crash the host process.
 */
object Enforcer {

    private const val TAG = "Enforcer"
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val mutex = Mutex()

    @Volatile private var currentPackage: String? = null

    private var overlay: OverlayController? = null
    private var lock: DeviceLockController? = null
    private var repo: AppLimitRepository? = null

    fun init(context: Context) {
        val app = context.applicationContext
        if (repo == null) repo = AppLimitRepository.get(app)
        if (overlay == null) overlay = OverlayController(app)
        if (lock == null) lock = DeviceLockController(app)
    }

    fun onForegroundChanged(context: Context, pkg: String?) {
        init(context)
        currentPackage = pkg
        val app = context.applicationContext
        scope.launch { runCatching { evaluate(app, pkg) }.onFailure { Log.e(TAG, "evaluate failed", it) } }
    }

    fun recheck(context: Context) {
        init(context)
        val app = context.applicationContext
        scope.launch { runCatching { evaluate(app, currentPackage) }.onFailure { Log.e(TAG, "recheck failed", it) } }
    }

    private suspend fun evaluate(context: Context, pkg: String?): Unit = mutex.withLock {
        val repository = repo ?: return@withLock

        // Never block our own youth/parent portal.
        if (pkg == context.packageName) {
            withContext(Dispatchers.Main) { safeHide() }
            return@withLock
        }

        val decision = repository.evaluate(pkg)
        Log.d(TAG, "pkg=$pkg action=${decision.action} reason=${decision.reason}")

        withContext(Dispatchers.Main) {
            when (decision.action) {
                EnforcementAction.ALLOW -> safeHide()

                EnforcementAction.BLOCK_APP -> {
                    if (decision.reason.contains("Einstellungen")) {
                        // Don't just cover Settings — eject the child to the home
                        // screen so they can't reach the toggle that would disable
                        // us. Then hide any overlay.
                        AppMonitorAccessibilityService.bounceHome()
                        safeHide()
                    } else {
                        val remaining = (decision.dailyLimitMinutes - decision.limitedUsedMinutes)
                            .coerceAtLeast(0)
                        val msg = if (decision.reason.contains("gesperrt")) {
                            "Diese App ist von deinen Eltern gesperrt."
                        } else {
                            "Tageslimit erreicht ($remaining Min. übrig). Komm morgen wieder!"
                        }
                        runCatching { overlay?.showBlock("Limit erreicht", msg) }
                    }
                }

                EnforcementAction.LOCK_DEVICE -> {
                    // Persist only for the hard daily budget, not for Ruhezeit
                    // (which clears itself once the window reopens).
                    if (decision.persistentLock) {
                        runCatching { repository.settingsStore.setDeviceLockedToday(true) }
                    }
                    val level = runCatching { lock?.enforceFullLock() }.getOrNull()
                    val isRuhezeit = decision.reason.contains("Ruhezeit")
                    val title = if (isRuhezeit) "Ruhezeit" else "Gerät gesperrt"
                    val body = if (isRuhezeit) {
                        "Jetzt ist Ruhezeit. Die Apps sind bis zum nächsten Zeitfenster gesperrt."
                    } else {
                        "Die tägliche Gesamt-Nutzungszeit ist aufgebraucht " +
                            "(${decision.totalUsedMinutes} Min.)."
                    }
                    val suffix = if (level == DeviceLockController.LockLevel.OVERLAY_ONLY) {
                        "\n(Overlay-Sperre)"
                    } else ""
                    runCatching { overlay?.showFullLock(title, body + suffix) }
                }
            }
        }
    }

    private fun safeHide() {
        runCatching { overlay?.hide() }
    }

    fun clearOverlay() {
        safeHide()
    }
}
