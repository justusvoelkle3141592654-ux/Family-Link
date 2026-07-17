package com.applimit.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import com.applimit.domain.UsageStatsReader
import com.applimit.service.DeviceLockController

/**
 * Central place that knows how to check and request every special-access
 * permission (Prompt Punkt 5). None of these can be granted by a normal runtime
 * dialog — each opens the relevant Settings page for the parent to toggle by
 * hand, which is exactly why the onboarding flow exists.
 */
class PermissionsHelper(private val context: Context) {

    fun hasUsageAccess(): Boolean = UsageStatsReader(context).hasUsageAccess()

    fun hasOverlay(): Boolean = Settings.canDrawOverlays(context)

    fun hasAccessibility(): Boolean {
        val expected = "${context.packageName}/com.applimit.service.AppMonitorAccessibilityService"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun hasDeviceAdmin(): Boolean = DeviceLockController(context).isDeviceAdminActive()

    fun openUsageAccessSettings() = start(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))

    fun openOverlaySettings() = start(
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}"),
        ),
    )

    fun openAccessibilitySettings() = start(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))

    fun openDeviceAdmin() {
        val intent = DeviceLockController(context).enableAdminIntent(
            "App-Limit benötigt Geräteadministrator-Rechte, um bei Erreichen der " +
                "Gesamt-Nutzungszeit den Bildschirm zu sperren.",
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun start(intent: Intent) {
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
