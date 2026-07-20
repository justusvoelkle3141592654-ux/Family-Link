package com.applimit.service

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
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

    private val handler = Handler(Looper.getMainLooper())
    private val clockTick = object : Runnable {
        override fun run() {
            updateClock()
            handler.postDelayed(this, 1000)
        }
    }

    private fun updateClock() {
        val v = overlayView?.findViewWithTag<TextView>(TAG_CLOCK) ?: return
        val now = java.util.Calendar.getInstance()
        v.text = String.format(
            "%02d:%02d",
            now.get(java.util.Calendar.HOUR_OF_DAY),
            now.get(java.util.Calendar.MINUTE),
        )
    }

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
            if (fullLock) {
                updateClock()
                handler.removeCallbacks(clockTick)
                handler.postDelayed(clockTick, 1000)
            }
        } catch (_: Exception) {
            overlayView = null
        }
    }

    fun hide() {
        handler.removeCallbacks(clockTick)
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

    private fun buildView(title: String, message: String, fullLock: Boolean): View =
        if (fullLock) buildFullLock(title, message) else buildBlock(title, message)

    /**
     * Family-Link-style full-screen lock: a calm blue gradient, a big glyph in a
     * soft circle, the reason, and two clear actions (Phone + open App-Limit).
     */
    private fun buildFullLock(title: String, message: String): View {
        val root = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            isClickable = true
            isFocusable = true
            setPadding(dp(32), dp(32), dp(32), dp(32))
            background = GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                intArrayOf(Color.parseColor("#12245C"), Color.parseColor("#0A84FF")),
            )
        }

        // Large live clock at the top (spec: lock screen must show the time).
        val clock = TextView(context).apply {
            tag = TAG_CLOCK
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 56f)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif-light", android.graphics.Typeface.NORMAL)
            setPadding(0, 0, 0, dp(24))
        }

        val iconCircle = TextView(context).apply {
            text = "🔒"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 46f)
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#33FFFFFF"))
            }
            val s = dp(112)
            layoutParams = LinearLayout.LayoutParams(s, s)
        }
        val titleView = TextView(context).apply {
            tag = TAG_TITLE
            text = title
            setTextColor(Color.WHITE)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 28f)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.create("sans-serif", android.graphics.Typeface.BOLD)
            setPadding(0, dp(24), 0, 0)
        }
        val messageView = TextView(context).apply {
            tag = TAG_MESSAGE
            text = message
            setTextColor(Color.parseColor("#DCE6F7"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            gravity = Gravity.CENTER
            setLineSpacing(dp(4).toFloat(), 1f)
            setPadding(dp(8), dp(12), dp(8), dp(28))
        }

        root.addView(clock)
        root.addView(iconCircle)
        root.addView(titleView)
        root.addView(messageView)
        root.addView(pillButton("📞  Telefon öffnen", Color.parseColor("#34C759"), Color.WHITE) {
            startExternal(Intent(Intent.ACTION_DIAL).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        })
        root.addView(pillButton("App-Limit öffnen", Color.WHITE, Color.parseColor("#0A84FF")) {
            startExternal(
                Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        })
        val brand = TextView(context).apply {
            text = "App-Limit"
            setTextColor(Color.parseColor("#99FFFFFF"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            gravity = Gravity.CENTER
            setPadding(0, dp(28), 0, 0)
        }
        root.addView(brand)
        return root
    }

    /** Lighter blocking card shown over a single limited/blocked app. */
    private fun buildBlock(title: String, message: String): View {
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
        card.addView(TextView(context).apply {
            text = "⏰"
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 40f)
            gravity = Gravity.CENTER
        })
        card.addView(TextView(context).apply {
            tag = TAG_TITLE
            text = title
            setTextColor(Color.parseColor("#1C1C1E"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            gravity = Gravity.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, dp(8), 0, 0)
        })
        card.addView(TextView(context).apply {
            tag = TAG_MESSAGE
            text = message
            setTextColor(Color.parseColor("#8E8E93"))
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 15f)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, 0)
        })
        root.addView(card)
        return root
    }

    private fun pillButton(label: String, bg: Int, textColor: Int, onClick: () -> Unit): Button {
        return Button(context).apply {
            text = label
            isAllCaps = false
            setTextColor(textColor)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 17f)
            typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
            stateListAnimator = null
            background = GradientDrawable().apply {
                setColor(bg)
                cornerRadius = dp(16).toFloat()
            }
            val lp = LinearLayout.LayoutParams(dp(300), dp(54)).apply { topMargin = dp(14) }
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
        private const val TAG_CLOCK = "overlay_clock"

        @Suppress("unused")
        private fun overlayUri(context: Context): Uri = Uri.parse("package:${context.packageName}")
    }
}
