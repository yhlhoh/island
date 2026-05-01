package com.island.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import com.island.app.R
import com.island.app.media.MediaSessionManager
import com.island.app.view.IslandViewController

class IslandOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var overlayView: View? = null
    private var isExpanded = false
    private lateinit var islandViewController: IslandViewController
    private lateinit var mediaSessionManager: MediaSessionManager

    private val collapsedWidth get() = dpToPx(120)
    private val collapsedHeight get() = dpToPx(36)
    private val expandedWidth get() = dpToPx(300)
    private val expandedHeight get() = dpToPx(120)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager
        showCollapsedIsland()

        mediaSessionManager = MediaSessionManager(this)
        mediaSessionManager.init(object : MediaSessionManager.Callback {
            override fun onMetadataChanged(metadata: android.media.MediaMetadata?) {
                islandViewController.updateMedia(metadata, mediaSessionManager.getPlaybackState())
            }
            override fun onPlaybackStateChanged(state: android.media.session.PlaybackState?) {
                islandViewController.updateMedia(mediaSessionManager.getCurrentMetadata(), state)
            }
        })

        IslandNotificationListener.setCallback(object : IslandNotificationListener.NotificationCallback {
            override fun onNotificationPosted(data: IslandNotificationListener.NotificationData) {
                islandViewController.showNotification(data)
            }
            override fun onNotificationRemoved() {}
        })
    }

    private fun showCollapsedIsland() {
        val params = buildLayoutParams(collapsedWidth, collapsedHeight)
        val view = LayoutInflater.from(this).inflate(R.layout.island_collapsed, null)
        view.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_UP) {
                toggleExpanded()
            }
            true
        }
        overlayView = view
        windowManager.addView(view, params)

        val expandedView = LayoutInflater.from(this).inflate(R.layout.island_expanded, null)
        islandViewController = IslandViewController(expandedView, mediaSessionManager)
    }

    private fun toggleExpanded() {
        val view = overlayView ?: return
        if (!isExpanded) {
            windowManager.removeView(view)
            val expandedView = LayoutInflater.from(this).inflate(R.layout.island_expanded, null)
            expandedView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    toggleExpanded()
                }
                true
            }
            islandViewController = IslandViewController(expandedView, mediaSessionManager)
            val params = buildLayoutParams(expandedWidth, expandedHeight)
            overlayView = expandedView
            windowManager.addView(expandedView, params)
            isExpanded = true
        } else {
            windowManager.removeView(view)
            val collapsedView = LayoutInflater.from(this).inflate(R.layout.island_collapsed, null)
            collapsedView.setOnTouchListener { _, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    toggleExpanded()
                }
                true
            }
            val params = buildLayoutParams(collapsedWidth, collapsedHeight)
            overlayView = collapsedView
            windowManager.addView(collapsedView, params)
            isExpanded = false
        }
    }

    private fun buildLayoutParams(width: Int, height: Int): WindowManager.LayoutParams {
        return WindowManager.LayoutParams(
            width,
            height,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP or Gravity.CENTER_HORIZONTAL
            x = 0
            y = dpToPx(8)
            layoutInDisplayCutoutMode =
                WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
    }

    private fun dpToPx(dp: Int): Int {
        return (dp * resources.displayMetrics.density).toInt()
    }

    override fun onDestroy() {
        super.onDestroy()
        overlayView?.let { windowManager.removeView(it) }
        mediaSessionManager.release()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Island Overlay",
            NotificationManager.IMPORTANCE_LOW
        ).apply { description = "Dynamic Island overlay service" }
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(channel)
    }

    private fun buildForegroundNotification(): Notification {
        val notification = Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Island overlay is active")
            .setSmallIcon(R.drawable.ic_play)
            .build()
        return notification
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "island_overlay_channel"
    }
}
