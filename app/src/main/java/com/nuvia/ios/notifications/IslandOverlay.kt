package com.nuvia.ios.notifications

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.LinearLayout
import android.widget.TextView
import android.os.Handler
import android.os.Looper
import android.view.WindowManager.LayoutParams
import com.nuvia.ios.ui.theme.NuviaPurple

/**
 * A minimal notification pill shown only after the user grants Android's
 * Display over other apps permission. One overlay is reused so alerts never stack.
 */
object IslandOverlay {
    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var currentView: View? = null
    private var hideTask: Runnable? = null

    fun show(context: Context, appName: String, message: String, durationSeconds: Long? = null) {
        val appContext = context.applicationContext
        if (!Settings.canDrawOverlays(appContext)) return

        val prefs = appContext.getSharedPreferences("nuvia_settings", Context.MODE_PRIVATE)
        if (!prefs.getBoolean("island_enabled", true) && appName != "Nuvia preview") return

        val duration = durationSeconds ?: prefs.getFloat("duration_seconds", 5f).toLong()
        val showPreview = prefs.getBoolean("show_preview", true)
        val compact = prefs.getBoolean("compact_island", false)

        handler.post {
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            windowManager = wm
            removeCurrent(wm)

            val container = LinearLayout(appContext).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(dp(appContext, if (compact) 13 else 17), dp(appContext, if (compact) 8 else 11), dp(appContext, if (compact) 15 else 19), dp(appContext, if (compact) 8 else 11))
                background = GradientDrawable().apply {
                    shape = GradientDrawable.RECTANGLE
                    cornerRadius = dp(appContext, 32).toFloat()
                    setColor(Color.rgb(5, 7, 12))
                    setStroke(dp(appContext, 1), Color.rgb(48, 52, 68))
                }
                elevation = dp(appContext, 12).toFloat()
            }

            val dot = View(appContext).apply {
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(Color.rgb(155, 125, 246))
                }
            }
            container.addView(dot, LinearLayout.LayoutParams(dp(appContext, 9), dp(appContext, 9)))

            val textColumn = LinearLayout(appContext).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(appContext, 10), 0, 0, 0)
            }
            val title = TextView(appContext).apply {
                text = appName.ifBlank { "Notification" }
                setTextColor(Color.WHITE)
                textSize = if (compact) 11f else 12f
                typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
                maxLines = 1
                ellipsize = android.text.TextUtils.TruncateAt.END
            }
            textColumn.addView(title)
            if (showPreview && message.isNotBlank()) {
                val body = TextView(appContext).apply {
                    text = message
                    setTextColor(Color.rgb(187, 192, 207))
                    textSize = if (compact) 10f else 11f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                }
                textColumn.addView(body)
            }
            container.addView(textColumn, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

            val params = LayoutParams(
                dp(appContext, if (compact) 230 else 280),
                LayoutParams.WRAP_CONTENT,
                LayoutParams.TYPE_APPLICATION_OVERLAY,
                LayoutParams.FLAG_NOT_FOCUSABLE or
                    LayoutParams.FLAG_NOT_TOUCH_MODAL or
                    LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
                y = dp(appContext, 42)
            }

            runCatching {
                wm.addView(container, params)
                currentView = container
                val task = Runnable { removeCurrent(wm) }
                hideTask = task
                handler.postDelayed(task, duration.coerceIn(2L, 10L) * 1000L)
            }.onFailure {
                currentView = null
            }
        }
    }

    fun dismiss() {
        handler.post {
            windowManager?.let { removeCurrent(it) }
        }
    }

    private fun removeCurrent(wm: WindowManager) {
        hideTask?.let(handler::removeCallbacks)
        hideTask = null
        currentView?.let { view -> runCatching { wm.removeView(view) } }
        currentView = null
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()
}
