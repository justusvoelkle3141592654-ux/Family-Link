package com.applimit.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Restarts the enforcement service after a reboot so limits stay enforced. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action == Intent.ACTION_BOOT_COMPLETED) {
            EnforcementService.start(context)
        }
    }
}
