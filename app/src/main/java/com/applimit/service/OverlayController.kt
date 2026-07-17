package com.applimit.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat

/**
 * Draws / removes the blocking overlay via SYSTEM_ALERT_WINDOW (Prompt Punkt 4).
 *
 * The overlay is fully opaque and consumes touches, so the app underneath is
 * effectively unusable. Two variants:
 *   - showBlock(): covers the current limited/blocked app with a message.
 *   - showFullLock(): full-device overlay lock when the total budget is hit.
 *
 * Honest limitation: an overlay cannot intercept the hardware power button or
 * emergency dialer (see DeviceLockController). For that a Device-Owner Lock-Task
 * setup is required.
 */
class OverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun showBlock(title: String, message: String) {
        show(title, message, fullLock = false)
    }

    fun showFullLock(title: String, message: String) {
        show(title, message, fullLock = true)
    }

    private fun show(title: String, message: String, fullLock: Boolean) {
        if (!canDrawOverlays()) return
        // Already showing? Just update the texts.
        overlayView?.let { existing ->
            existing.findViewById<TextView>(ID_TITLE)?.text = title
            existing.findViewById<TextView>(ID_MESSAGE)?.text = message
            return
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            // FLAG_NOT_FOCUSABLE is intentionally NOT set so the overlay can also
            // swallow the back key while it is showing.
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE,
        ).apply { gravity = Gravity.CENTER }

        val view = buildView(title, message, fullLock)
        overlayView = view
        windowManager.addView(view, params)
    }

    fun hide() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: IllegalArgumentException) {
                // already removed
            }
        }
        overlayView = null
    }

    fun isShowing(): Boolean = overlayView != null

    /**
     * Built programmatically (no XML dependency) so the service stays
     * self-contained. Styled to match the light iOS look of the app.
     */
    private fun buildView(title: String, message: String, fullLock: Boolean): View {
        val density = context.resources.displayMetrics.density
        fun dp(v: Int) = (v * density).toInt()

        val root = FrameLayout(context).apply {
            setBackgroundColor(if (fullLock) Color.parseColor("#F2F2F7") else Color.parseColor("#E6F2F2F7"))
            isClickable = true // swallow touches
            isFocusable = true
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(Color.WHITE)
            setPadding(dp(28), dp(32), dp(28), dp(32))
            val lp = FrameLayout.LayoutParams(dp(300), FrameLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
            }
            layoutParams = lp
        }

        val titleView = TextView(context).apply {
            id = ID_TITLE
            text = title
            setTextColor(Color.parseColor("#1C1C1E"))
            textSize = 20f
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val messageView = TextView(context).apply {
            id = ID_MESSAGE
            text = message
            setTextColor(Color.parseColor("#8E8E93"))
            textSize = 15f
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        card.addView(titleView)
        card.addView(messageView)
        root.addView(card)
        // Give the card rounded corners programmatically.
        card.background = ContextCompat.getDrawable(context, android.R.color.white)
        return root
    }

    companion object {
        private val ID_TITLE = View.generateViewId()
        private val ID_MESSAGE = View.generateViewId()

        @Suppress("unused")
        private fun unusedInflaterHint(context: Context) = LayoutInflater.from(context)
    }
}
