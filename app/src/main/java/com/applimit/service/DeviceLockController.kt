package com.applimit.service

import android.app.admin.DevicePolicyManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log

/**
 * Controls the full-device lock (Prompt Punkt 3 & 5) and documents, honestly,
 * which lock strength is actually achievable on the current device.
 *
 * ┌─────────────────────────────────────────────────────────────────────────┐
 * │ LOCK LEVELS                                                               │
 * ├─────────────────────────────────────────────────────────────────────────┤
 * │ LEVEL 1 – OVERLAY LOCK (always available)                                 │
 * │   A full-screen SYSTEM_ALERT_WINDOW overlay covers everything. It blocks  │
 * │   the UI visually and re-shows itself on every foreground change via the  │
 * │   AccessibilityService. The Home/Recents buttons cannot dismiss the       │
 * │   blocked app because we immediately re-cover it. HOWEVER the power        │
 * │   button and the emergency dialer remain reachable — on stock Android      │
 * │   this cannot be fully prevented without Device Owner.                    │
 * │                                                                           │
 * │ LEVEL 2 – DEVICE-ADMIN lockNow() (available once admin is enabled)         │
 * │   Forces the phone to its secure lockscreen. The child must re-enter the  │
 * │   device passcode to get back in. Still does not disable power/emergency. │
 * │                                                                           │
 * │ LEVEL 3 – DEVICE OWNER (only on a factory-reset provisioned device)        │
 * │   The only truly uncircumventable level, exactly like Google Family Link. │
 * │   Enables Lock Task / kiosk mode, disabling Home, Recents, status bar and │
 * │   preventing uninstall. Requires QR/NFC/afw provisioning at setup time —   │
 * │   it cannot be turned on after the fact on an already-configured phone.    │
 * └─────────────────────────────────────────────────────────────────────────┘
 *
 * This app targets LEVEL 1 + LEVEL 2 out of the box (installable on any normal
 * phone). LEVEL 3 is documented for the parent in docs/DECISIONS.md but is not
 * auto-provisioned, because that is physically impossible post-setup.
 */
class DeviceLockController(private val context: Context) {

    private val dpm =
        context.getSystemService(Context.DEVICE_POLICY_SERVICE) as DevicePolicyManager
    private val adminComponent =
        ComponentName(context, AppLimitDeviceAdminReceiver::class.java)

    enum class LockLevel { OVERLAY_ONLY, DEVICE_ADMIN, DEVICE_OWNER }

    /** The strongest lock currently achievable on this device. */
    fun availableLockLevel(): LockLevel = when {
        dpm.isDeviceOwnerApp(context.packageName) -> LockLevel.DEVICE_OWNER
        dpm.isAdminActive(adminComponent) -> LockLevel.DEVICE_ADMIN
        else -> LockLevel.OVERLAY_ONLY
    }

    fun isDeviceAdminActive(): Boolean = dpm.isAdminActive(adminComponent)

    /** Intent that asks the user to enable device admin (used in onboarding). */
    fun enableAdminIntent(explanation: String): Intent =
        Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN).apply {
            putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent)
            putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, explanation)
        }

    /**
     * Enforces the strongest lock available. Returns which level was applied so
     * the caller can inform the parent transparently.
     */
    fun enforceFullLock(): LockLevel {
        val level = availableLockLevel()
        when (level) {
            LockLevel.DEVICE_OWNER, LockLevel.DEVICE_ADMIN -> {
                try {
                    dpm.lockNow()
                } catch (e: SecurityException) {
                    Log.e(TAG, "lockNow() denied", e)
                }
            }
            LockLevel.OVERLAY_ONLY -> {
                // Nothing to call here — the EnforcementService shows the
                // full-screen overlay instead. This is the honest fallback.
                Log.i(TAG, "Overlay-only lock (no device admin granted)")
            }
        }
        return level
    }

    fun openSecuritySettings() {
        context.startActivity(
            Intent(Settings.ACTION_SECURITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    /**
     * Anti-bypass hardening (guest profile, user switch, safe mode, factory
     * reset).
     *
     * HONEST LIMITATION: these user restrictions can ONLY be applied when the
     * app is Device Owner. On a normal install they silently do nothing — stock
     * Android simply does not let a regular app block the guest profile or safe
     * mode. What we CAN still do without Device Owner is block the Settings app
     * via the overlay (see LimitEvaluator) and, as an active device admin, stay
     * un-uninstallable until the admin is disabled. See docs/DECISIONS.md.
     */
    fun applyBypassRestrictions(enable: Boolean) {
        if (!dpm.isDeviceOwnerApp(context.packageName)) return
        val restrictions = listOf(
            "no_add_user",          // UserManager.DISALLOW_ADD_USER
            "no_user_switch",       // DISALLOW_USER_SWITCH
            "no_safe_boot",         // DISALLOW_SAFE_BOOT
            "no_factory_reset",     // DISALLOW_FACTORY_RESET
        )
        for (key in restrictions) {
            try {
                if (enable) dpm.addUserRestriction(adminComponent, key)
                else dpm.clearUserRestriction(adminComponent, key)
            } catch (e: SecurityException) {
                Log.w(TAG, "restriction $key failed", e)
            }
        }
    }

    companion object {
        private const val TAG = "DeviceLockController"
    }
}
