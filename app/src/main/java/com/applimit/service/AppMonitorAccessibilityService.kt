package com.applimit.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Detects foreground-app changes in real time (Prompt Punkt 5). On every window
 * state change we hand the new package to [Enforcer], which decides whether to
 * show a blocking overlay or trigger the device lock.
 *
 * Only WINDOW_STATE/WINDOWS_CHANGED events are requested (see
 * res/xml/accessibility_service_config.xml), and no window content is stored or
 * transmitted — we only read the package name of the frontmost window.
 */
class AppMonitorAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        super.onServiceConnected()
        Enforcer.init(this)
        // Make sure the time-driven checker is running too (started as a proper
        // foreground service so it survives and can host the overlay).
        EnforcementService.start(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            event.eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
        ) return

        val pkg = event.packageName?.toString() ?: return
        // Ignore system UI transitions (status bar, keyboard, launcher popups
        // fire these too, but package name still tells us the real foreground).
        Enforcer.onForegroundChanged(this, pkg)
    }

    override fun onInterrupt() { /* no-op */ }
}
