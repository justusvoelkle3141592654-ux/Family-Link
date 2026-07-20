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
                        // Eject the child to the home screen so they can't reach
                        // the toggle that would disable us.
                        AppMonitorAccessibilityService.bounceHome()
                        safeHide()
                    } else {
                        val msg = when {
                            decision.reason.contains("Individuelles") ->
                                "Das Zeitlimit für diese App ist aufgebraucht " +
                                    "(${decision.individualUsedSec / 60}/${decision.individualLimitSec / 60} Min.)."
                            decision.reason.contains("Allgemeines") ->
                                "Dein allgemeines Zeitlimit ist aufgebraucht " +
                                    "(${decision.generalUsedSec / 60}/${decision.generalLimitSec / 60} Min.)."
                            decision.reason.contains("Globales") ->
                                "Deine gesamte Bildschirmzeit ist aufgebraucht " +
                                    "(${decision.globalUsedSec / 60}/${decision.globalLimitSec / 60} Min.)."
                            else ->
                                "Diese App ist von deinen Eltern gesperrt."
                        }
                        runCatching { overlay?.showBlock(decision.reason, msg) }
                    }
                }

                EnforcementAction.LOCK_DEVICE -> {
                    // Reached by Ruhezeit and by the general/global limit → lock
                    // the whole device (lockNow where admin is granted) and show
                    // the full-screen lock. Phone + App-Limit stay reachable.
                    val level = runCatching { lock?.enforceFullLock() }.getOrNull()
                    val suffix = if (level == DeviceLockController.LockLevel.OVERLAY_ONLY) {
                        "\n(Tipp: Geräteadministrator aktivieren für eine echte Sperre.)"
                    } else ""
                    val (title, body) = when {
                        decision.reason.contains("Ruhezeit") ->
                            "Ruhezeit" to "Jetzt ist Ruhezeit. Die Apps sind bis zum nächsten Zeitfenster gesperrt."
                        decision.reason.contains("Globales") ->
                            "Zeit ist um" to "Deine gesamte Bildschirmzeit ist aufgebraucht " +
                                "(${decision.globalUsedSec / 60}/${decision.globalLimitSec / 60} Min.)."
                        else ->
                            "Zeit ist um" to "Dein Zeitlimit ist aufgebraucht " +
                                "(${decision.generalUsedSec / 60}/${decision.generalLimitSec / 60} Min.)."
                    }
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
