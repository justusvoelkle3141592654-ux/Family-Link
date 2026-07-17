package com.applimit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import com.applimit.R
import com.applimit.data.repository.AppLimitRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * Foreground service that (a) keeps the app alive to host the overlay and
 * (b) re-evaluates limits on a timer, so a limit reached while the child simply
 * keeps staring at an already-open app is still caught (accessibility events
 * only fire on window changes).
 *
 * It also performs the midnight reset (Prompt Punkt 3 / Rückfrage 3): when the
 * calendar day rolls over it clears the day's usage flags.
 */
class EnforcementService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default)
    private var loopJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        Enforcer.init(this)
        // Android 14 (API 34) requires the foreground-service type to be passed
        // explicitly for a "specialUse" service; older versions use the 2-arg form.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                NOTIF_ID,
                buildNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIF_ID, buildNotification())
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (loopJob?.isActive != true) {
            loopJob = scope.launch { monitorLoop() }
        }
        return START_STICKY
    }

    private suspend fun monitorLoop() {
        val repo = AppLimitRepository.get(this)
        val lockController = DeviceLockController(this)
        while (scope.isActive) {
            runCatching {
                val settings = repo.settingsStore.current()
                maybeMidnightReset(repo, settings)
                // Apply/lift anti-bypass restrictions (only effective as Device
                // Owner; a harmless no-op otherwise).
                lockController.applyBypassRestrictions(settings.protectionEnabled)
                Enforcer.recheck(this)
            }
            delay(CHECK_INTERVAL_MS)
        }
    }

    /** Clears the day's counters/flags once when the local date changes. */
    private suspend fun maybeMidnightReset(repo: AppLimitRepository, settings: com.applimit.data.prefs.AppSettings) {
        if (!settings.autoResetAtMidnight) return
        val today = LocalDate.now().dayOfYear
        if (settings.lastResetDayOfYear != today) {
            repo.settingsStore.setDeviceLockedToday(false)
            repo.settingsStore.setLastResetDayOfYear(today)
            // Usage numbers themselves come from UsageStatsManager scoped to
            // "since midnight", so they reset automatically with the clock.
            Enforcer.clearOverlay()
        }
    }

    override fun onDestroy() {
        loopJob?.cancel()
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.enforcement_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            )
            nm.createNotificationChannel(channel)
        }
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.enforcement_notification_title))
            .setContentText(getString(R.string.enforcement_notification_text))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .build()
    }

    companion object {
        private const val NOTIF_ID = 42
        private const val CHANNEL_ID = "applimit_enforcement"
        // Check often so limits/Ruhezeit take effect quickly even when the
        // child just stays inside one already-open app (no window events).
        private const val CHECK_INTERVAL_MS = 5_000L

        fun start(context: Context) {
            val intent = Intent(context, EnforcementService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }
}
