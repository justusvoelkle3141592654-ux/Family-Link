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
 * and the EnforcementService (time-driven). Keeps the currently foregrounded
 * package and applies the [com.applimit.domain.LimitEvaluator] decision by
 * showing/hiding the overlay or triggering the device lock.
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
        if (repo == null) repo = AppLimitRepository.get(context)
        if (overlay == null) overlay = OverlayController(context)
        if (lock == null) lock = DeviceLockController(context)
    }

    /** Called by the accessibility service on every foreground app change. */
    fun onForegroundChanged(context: Context, pkg: String?) {
        init(context)
        currentPackage = pkg
        scope.launch { evaluate(context, pkg) }
    }

    /** Called periodically by the enforcement service to catch time-based limits. */
    fun recheck(context: Context) {
        init(context)
        scope.launch { evaluate(context, currentPackage) }
    }

    private suspend fun evaluate(context: Context, pkg: String?) = mutex.withLock {
        val repository = repo ?: return
        // Never block our own UI.
        if (pkg == context.packageName) {
            withContext(Dispatchers.Main) { overlay?.hide() }
            return
        }

        val decision = repository.evaluate(pkg)
        Log.d(TAG, "pkg=$pkg action=${decision.action} reason=${decision.reason}")

        withContext(Dispatchers.Main) {
            when (decision.action) {
                EnforcementAction.ALLOW -> overlay?.hide()

                EnforcementAction.BLOCK_APP -> {
                    val remaining = (decision.dailyLimitMinutes - decision.limitedUsedMinutes)
                        .coerceAtLeast(0)
                    val msg = if (decision.reason.contains("gesperrt")) {
                        "Diese App ist von deinen Eltern gesperrt."
                    } else {
                        "Tageslimit erreicht ($remaining Min. übrig). " +
                            "Komm morgen wieder!"
                    }
                    overlay?.showBlock("Limit erreicht", msg)
                }

                EnforcementAction.LOCK_DEVICE -> {
                    // Persist the tripped state so it survives restarts until the
                    // midnight reset (or a parent emergency reset).
                    repository.settingsStore.setDeviceLockedToday(true)
                    // Try the strongest available lock, then also show the overlay
                    // as the honest fallback (overlay-only devices).
                    val level = lock?.enforceFullLock()
                    overlay?.showFullLock(
                        "Gerät gesperrt",
                        "Die tägliche Gesamt-Nutzungszeit ist aufgebraucht." +
                            if (level == DeviceLockController.LockLevel.OVERLAY_ONLY) {
                                "\n(Overlay-Sperre)"
                            } else "",
                    )
                }
            }
        }
    }

    fun clearOverlay() {
        overlay?.hide()
    }
}
