package com.example.service

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Vibrator
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import java.util.Locale

class FloatingFocusService : Service() {

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_UPDATE = "ACTION_UPDATE"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_TOGGLE_PAUSE = "ACTION_TOGGLE_PAUSE"

        const val EXTRA_REMAINING_SECONDS = "EXTRA_REMAINING_SECONDS"
        const val EXTRA_TOTAL_SECONDS = "EXTRA_TOTAL_SECONDS"
        const val EXTRA_IS_RUNNING = "EXTRA_IS_RUNNING"
        const val EXTRA_DOC_TITLE = "EXTRA_DOC_TITLE"

        private const val NOTIFICATION_ID = 9021
        private const val CHANNEL_ID = "focus_room_channel"

        var isServiceActive = false
            private set

        fun start(context: Context, remainingSeconds: Int, isRunning: Boolean, title: String? = null) {
            val intent = Intent(context, FloatingFocusService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_IS_RUNNING, isRunning)
                putExtra(EXTRA_DOC_TITLE, title ?: "Exam Focus Session")
            }
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.e("FloatingFocusService", "Error starting service: ${e.message}")
            }
        }

        fun update(context: Context, remainingSeconds: Int, isRunning: Boolean) {
            if (!isServiceActive) return
            val intent = Intent(context, FloatingFocusService::class.java).apply {
                action = ACTION_UPDATE
                putExtra(EXTRA_REMAINING_SECONDS, remainingSeconds)
                putExtra(EXTRA_IS_RUNNING, isRunning)
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }

        fun stop(context: Context) {
            val intent = Intent(context, FloatingFocusService::class.java).apply {
                action = ACTION_STOP
            }
            try {
                context.startService(intent)
            } catch (_: Exception) {}
        }
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var timerTextView: TextView? = null
    private var statusTextView: TextView? = null
    private var playPauseIcon: ImageView? = null

    private var remainingSeconds = 1500
    private var isRunning = false
    private var docTitle = "Exam Focus Room"

    private val handler = Handler(Looper.getMainLooper())
    private val tickRunnable = object : Runnable {
        override fun run() {
            if (isRunning && remainingSeconds > 0) {
                remainingSeconds--
                updateUI()
                updateNotification()
                if (remainingSeconds == 0) {
                    onTimerFinished()
                    return
                }
                handler.postDelayed(this, 1000)
            }
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        isServiceActive = true
        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action ?: ACTION_START

        when (action) {
            ACTION_START -> {
                remainingSeconds = intent?.getIntExtra(EXTRA_REMAINING_SECONDS, remainingSeconds) ?: remainingSeconds
                isRunning = intent?.getBooleanExtra(EXTRA_IS_RUNNING, false) ?: false
                docTitle = intent?.getStringExtra(EXTRA_DOC_TITLE) ?: docTitle

                startForeground(NOTIFICATION_ID, buildNotification())
                setupOverlayView()
                handler.removeCallbacks(tickRunnable)
                if (isRunning) {
                    handler.postDelayed(tickRunnable, 1000)
                }
            }
            ACTION_UPDATE -> {
                val newRemaining = intent?.getIntExtra(EXTRA_REMAINING_SECONDS, remainingSeconds) ?: remainingSeconds
                val newRunning = intent?.getBooleanExtra(EXTRA_IS_RUNNING, isRunning) ?: isRunning
                remainingSeconds = newRemaining
                isRunning = newRunning

                updateUI()
                updateNotification()

                handler.removeCallbacks(tickRunnable)
                if (isRunning && remainingSeconds > 0) {
                    handler.postDelayed(tickRunnable, 1000)
                }
            }
            ACTION_TOGGLE_PAUSE -> {
                isRunning = !isRunning
                updateUI()
                updateNotification()
                handler.removeCallbacks(tickRunnable)
                if (isRunning && remainingSeconds > 0) {
                    handler.postDelayed(tickRunnable, 1000)
                }
            }
            ACTION_STOP -> {
                stopSelf()
            }
        }

        return START_NOT_STICKY
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupOverlayView() {
        if (!Settings.canDrawOverlays(this)) {
            Log.w("FloatingFocusService", "SYSTEM_ALERT_WINDOW not granted, running in foreground notification only.")
            return
        }

        if (floatingView != null) return

        val layoutType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            layoutType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 24
            y = 120
        }

        // Programmatically build sleek floating pill matching Material 3 theme
        val context = this
        val rootLayout = FrameLayout(context).apply {
            val bg = GradientDrawable().apply {
                shape = GradientDrawable.RECTANGLE
                cornerRadius = 36f
                setColor(Color.parseColor("#1E2024"))
                setStroke(3, Color.parseColor("#10B981"))
            }
            background = bg
            setPadding(18, 12, 18, 12)
            elevation = 16f
        }

        val row = LinearLayout(context).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        // Timer Text
        val timerTv = TextView(context).apply {
            text = formatTime(remainingSeconds)
            setTextColor(Color.WHITE)
            textSize = 15f
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(8, 0, 8, 0)
        }
        timerTextView = timerTv
        row.addView(timerTv)

        // Status badge
        val statusTv = TextView(context).apply {
            text = if (isRunning) "●" else "❚❚"
            setTextColor(if (isRunning) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
            textSize = 12f
            setPadding(4, 0, 8, 0)
        }
        statusTextView = statusTv
        row.addView(statusTv)

        // Open App Button
        val expandIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_crop)
            setColorFilter(Color.parseColor("#CBD5E1"))
            setPadding(6, 6, 6, 6)
            setOnClickListener {
                bringAppToFront()
            }
        }
        row.addView(expandIcon)

        // Close Button
        val closeIcon = ImageView(context).apply {
            setImageResource(android.R.drawable.ic_menu_close_clear_cancel)
            setColorFilter(Color.parseColor("#94A3B8"))
            setPadding(6, 6, 6, 6)
            setOnClickListener {
                stopSelf()
            }
        }
        row.addView(closeIcon)

        rootLayout.addView(row)

        // Touch & Drag Handling
        var initialX = 0
        var initialY = 0
        var initialTouchX = 0f
        var initialTouchY = 0f
        var isClick = true

        rootLayout.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialX = params.x
                    initialY = params.y
                    initialTouchX = event.rawX
                    initialTouchY = event.rawY
                    isClick = true
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = (event.rawX - initialTouchX).toInt()
                    val dy = (event.rawY - initialTouchY).toInt()
                    if (Math.abs(dx) > 10 || Math.abs(dy) > 10) {
                        isClick = false
                    }
                    params.x = initialX + dx
                    params.y = initialY + dy
                    windowManager?.updateViewLayout(rootLayout, params)
                    true
                }
                MotionEvent.ACTION_UP -> {
                    if (isClick) {
                        bringAppToFront()
                    }
                    true
                }
                else -> false
            }
        }

        try {
            windowManager?.addView(rootLayout, params)
            floatingView = rootLayout
        } catch (e: Exception) {
            Log.e("FloatingFocusService", "Failed to add overlay window: ${e.message}")
        }
    }

    private fun updateUI() {
        timerTextView?.text = formatTime(remainingSeconds)
        statusTextView?.apply {
            text = if (isRunning) "●" else "❚❚"
            setTextColor(if (isRunning) Color.parseColor("#10B981") else Color.parseColor("#F59E0B"))
        }
    }

    private fun formatTime(secs: Int): String {
        val m = secs / 60
        val s = secs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", m, s)
    }

    private fun bringAppToFront(isTimeout: Boolean = false) {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("EXTRA_FOCUS_TIMEOUT", isTimeout)
        }
        startActivity(intent)
    }

    private fun onTimerFinished() {
        isRunning = false
        updateUI()

        // 1. Vibrate device for feedback
        try {
            val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            vibrator?.vibrate(longArrayOf(0, 500, 200, 500), -1)
        } catch (_: Exception) {}

        // 2. Automatically open MainActivity window right at 0:00!
        bringAppToFront(isTimeout = true)

        // 3. Update notification to show time up alert
        val notifyMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("EXTRA_FOCUS_TIMEOUT", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val timeoutNotification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("⏰ Focus Time Complete (0:00)!")
            .setContentText("Tap to review your paper evaluation & companion scheme.")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        notifyMgr.notify(NOTIFICATION_ID + 1, timeoutNotification)

        // Remove overlay
        removeOverlay()
        stopSelf()
    }

    private fun buildNotification(): Notification {
        val openIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_menu_recent_history)
            .setContentTitle("⏱ Focus Room: ${formatTime(remainingSeconds)}")
            .setContentText(if (isRunning) "Exam session active • Tap to open" else "Session paused")
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification() {
        val notifyMgr = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notifyMgr.notify(NOTIFICATION_ID, buildNotification())
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Focus Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows live countdown for active study focus sessions"
                enableLights(false)
                enableVibration(false)
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun removeOverlay() {
        if (floatingView != null && windowManager != null) {
            try {
                windowManager?.removeView(floatingView)
            } catch (_: Exception) {}
            floatingView = null
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceActive = false
        handler.removeCallbacks(tickRunnable)
        removeOverlay()
    }
}
