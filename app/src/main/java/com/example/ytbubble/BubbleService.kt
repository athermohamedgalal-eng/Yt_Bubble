package com.example.ytbubble

import android.app.Notification
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import kotlin.math.abs

class BubbleService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var bubbleContainer: LinearLayout
    private lateinit var popupMenu: LinearLayout
    private lateinit var layoutParams: WindowManager.LayoutParams

    private val handler = Handler(Looper.getMainLooper())
    private var isBubbleAdded = false
    private var isMenuOpen = false
    private var lastForegroundApp = ""

    private val checkForegroundRunnable = object : Runnable {
        override fun run() {
            val currentApp = getForegroundPackage()
            if (currentApp.isNotEmpty()) {
                lastForegroundApp = currentApp
            }

            if (lastForegroundApp == "com.google.android.youtube") {
                showBubble()
            } else {
                hideBubble()
            }
            handler.postDelayed(this, 800)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        val notification = Notification.Builder(this)
            .setContentTitle("YT Bubble Active")
            .setContentText("Waiting for YouTube...")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .build()
        startForeground(101, notification)

        setupBubbleView()
        handler.post(checkForegroundRunnable)
    }

    private fun setupBubbleView() {
        bubbleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }

        val bubbleCircle = TextView(this).apply {
            text = "YT"
            setTextColor(Color.WHITE)
            textSize = 14f
            gravity = Gravity.CENTER
            background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(Color.parseColor("#E62117"))
                setStroke(3, Color.WHITE)
            }
        }
        val circleParams = LinearLayout.LayoutParams(110, 110)
        bubbleContainer.addView(bubbleCircle, circleParams)

        popupMenu = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 20, 24, 20)
            visibility = View.GONE
            background = GradientDrawable().apply {
                cornerRadius = 24f
                setColor(Color.parseColor("#EE222222"))
            }
        }

        val btnDownloads = Button(this).apply {
            text = "Open Downloads Manager"
            setOnClickListener {
                toggleMenu()
                val intent = Intent(this@BubbleService, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                startActivity(intent)
            }
        }

        val btnHide = Button(this).apply {
            text = "Hide Now"
            setOnClickListener {
                toggleMenu()
                hideBubble()
            }
        }

        popupMenu.addView(btnDownloads)
        popupMenu.addView(btnHide)
        bubbleContainer.addView(popupMenu)

        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            @Suppress("DEPRECATION")
            WindowManager.LayoutParams.TYPE_PHONE
        }

        layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            x = 40
            y = 250
        }

        bubbleCircle.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        initialTouchX = event.rawX
                        initialTouchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX + (event.rawX - initialTouchX).toInt()
                        layoutParams.y = initialY + (event.rawY - initialTouchY).toInt()
                        if (isBubbleAdded) {
                            windowManager.updateViewLayout(bubbleContainer, layoutParams)
                        }
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = abs(event.rawX - initialTouchX)
                        val dy = abs(event.rawY - initialTouchY)
                        if (dx < 15 && dy < 15) {
                            toggleMenu()
                        }
                        return true
                    }
                }
                return false
            }
        })
    }

    private fun toggleMenu() {
        isMenuOpen = !isMenuOpen
        popupMenu.visibility = if (isMenuOpen) View.VISIBLE else View.GONE
        if (isBubbleAdded) {
            windowManager.updateViewLayout(bubbleContainer, layoutParams)
        }
    }

    private fun showBubble() {
        if (!isBubbleAdded) {
            try {
                windowManager.addView(bubbleContainer, layoutParams)
                isBubbleAdded = true
            } catch (_: Exception) {
            }
        }
    }

    private fun hideBubble() {
        if (isBubbleAdded) {
            try {
                isMenuOpen = false
                popupMenu.visibility = View.GONE
                windowManager.removeView(bubbleContainer)
                isBubbleAdded = false
            } catch (_: Exception) {
            }
        }
    }

    private fun getForegroundPackage(): String {
        val usm = getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val endTime = System.currentTimeMillis()
        val beginTime = endTime - 10_000
        val events = usm.queryEvents(beginTime, endTime)
        val event = UsageEvents.Event()
        var currentPkg = ""
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            if (event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                currentPkg = event.packageName
            }
        }
        return currentPkg
    }

    override fun onDestroy() {
        handler.removeCallbacks(checkForegroundRunnable)
        hideBubble()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
