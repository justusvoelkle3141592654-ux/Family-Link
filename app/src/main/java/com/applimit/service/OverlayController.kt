package com.applimit.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import com.applimit.MainActivity

/**
 * Draws / removes the blocking overlay via SYSTEM_ALERT_WINDOW (Prompt Punkt 4).
 *
 * Two variants:
 *   - showBlock():    a message card over a single limited/blocked app.
 *   - showFullLock(): a full-screen lock (budget reached / Ruhezeit) with two
 *                     actions, à la Family Link: open the Phone app (emergency
 *                     calls stay possible) and open the App-Limit portal (PIN).
 *
 * Everything is wrapped defensively so a WindowManager hiccup can never crash
 * the hosting service.
 */
class OverlayController(private val context: Context) {

    private val windowManager =
        context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

    private var overlayView: View? = null

    fun canDrawOverlays(): Boolean = Settings.canDrawOverlays(context)

    fun showBlock(title: String, message: String) = show(title, message, fullLock = false)

    fun showFullLock(title: String, message: String) = show(title, message, fullLock = true)

    private fun show(title: String, message: String, fullLock: Boolean) {
        if (!canDrawOverlays()) return

        // Already showing? Just refresh the texts instead of re-adding.
        overlayView?.let { existing ->
            (existing.findViewWithTag<TextView>(TAG_TITLE))?.text = title
            (existing.findViewWithTag<TextView>(TAG_MESSAGE))?.text = message
            return
        }

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_SYSTEM_ALERT
        }

        // Focusable so we can also swallow the Back key while blocking; buttons
        // remain tappable because the view is touchable (no NOT_TOUCHABLE flag).
        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE,
        ).apply { gravity = Gravity.CENTER }

        val view = buildView(title, message, fullLock)
        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (_: Exception) {
            overlayView = null
        }
    }

    fun hide() {
        overlayView?.let {
            try {
                windowManager.removeView(it)
            } catch (_: Exception) {
                // already gone
            }
        }
        overlayView = null
    }

    fun isShowing(): Boolean = overlayView != null

    private fun dp(v: Int): Int = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), context.resources.displayMetrics,
    ).toInt()

    private fun buildView(title: String, message: String, fullLock: Boolean): View {
        val root = FrameLayout(context).apply {
            setBackgroundColor(Color.parseColor("#F2F2F7"))
            isClickable = true
            isFocusable = true
        }

        val card = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            background = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = dp(20).toFloat()
            }
            setPadding(dp(28), dp(32), dp(28), dp(28))
            layoutParams = FrameLayout.LayoutParams(dp(320), FrameLayout.LayoutParams.WRAP_CONTENT)
                .apply { gravity = Gravity.CENTER }
        }

        val icon = TextView(context).apply {
            text = if (fullLock) "🔒" else "⏰"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            gravity = Gravity.CENTER
        }
        val titleView = TextView(context).apply {
            tag = TAG_TITLE
            text = title
            setTextColor(Color.parseColor("#1C1C1E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, 0)
        }
        val messageView = TextView(context).apply {
            tag = TAG_MESSAGE
            text = message
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        }

        card.addView(icon)
        card.addView(titleView)
        card.addView(messageView)

        if (fullLock) {
            card.addView(pillButton("📞 Telefon öffnen", Color.parseColor("#34C759")) {
                startExternal(
                    Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
            card.addView(pillButton("App-Limit öffnen", Color.parseColor("#0A84FF")) {
                startExternal(
                    Intent(context, MainActivity::class.java)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            })
        }

        root.addView(card)
        return root
    }

    private fun pillButton(label: String, color: Int, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            isAllCaps = false
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            background = GradientDrawable().apply {
                setColor(color)
                cornerRadius = dp(14).toFloat()
            }
            val lp = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(50),
            ).apply { topMargin = dp(12) }
            layoutParams = lp
            setOnClickListener { onClick() }
        }
    }

    private fun startExternal(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            // ignore – e.g. no dialer present
        }
    }

    companion object {
        private const val TAG_TITLE = "overlay_title"
        private const val TAG_MESSAGE = "overlay_message"

        @Suppress("unused")
        private fun overlayUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
    }
}
