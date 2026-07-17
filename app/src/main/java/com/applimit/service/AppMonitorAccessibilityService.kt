package com.applimit.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Detects foreground-app changes in real time (Prompt Punkt 5). On every window
 * state change we hand the new package to [Enforcer], which decides whether to
 * show a blocking overlay or trigger the device lock.
 *
 * It also exposes [bounceHome], which the Enforcer calls to eject the child from
 * the Android Settings app the instant it is opened while protection is on — so
 * they can't reach the toggles that would disable this app (overlay permission /
 * accessibility service). This is the strongest anti-tamper possible without
 * Device Owner; see docs/DECISIONS.md.
 */
class AppMonitorAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Enforcer.init(this)
        EnforcementService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return
        Enforcer.onForegroundChanged(this, pkg)
    }

    override fun onInterrupt() { /* no-op */ }

    override fun onDestroy() {
        if (instance === this) instance = null
        super.onDestroy()
    }

    companion object {
        @Volatile private var instance: AppMonitorAccessibilityService? = null

        /** Sends the user to the home screen (used to eject them from Settings). */
        fun bounceHome() {
            try {
                instance?.performGlobalAction(GLOBAL_ACTION_HOME)
            } catch (_: Exception) {
            }
        }

        /** Presses Back (used to back out of a blocked app). */
        fun bounceBack() {
            try {
                instance?.performGlobalAction(GLOBAL_ACTION_BACK)
            } catch (_: Exception) {
            }
        }
    }
}
