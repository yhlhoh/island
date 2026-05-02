package com.island.app.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import com.island.app.R
import com.island.app.media.MediaSessionManager
import com.island.app.view.IslandViewController
import com.island.app.view.SoundWaveView

class IslandOverlayService : Service() {

    private lateinit var windowManager: WindowManager
    private var isExpanded = false

    private var collapsedView: View? = null
    private var expandedView: View? = null

    private var collapsedSoundWave: SoundWaveView? = null
    private var collapsedAlbumArt: ImageView? = null

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

        val inflater = LayoutInflater.from(this)
        collapsedView = inflater.inflate(R.layout.island_collapsed, null).also { view ->
            collapsedSoundWave = view.findViewById(R.id.soundWaveCollapsed)
            collapsedAlbumArt = view.findViewById(R.id.ivCollapsedAlbumArt)
            view.setOnClickListener { toggleExpanded() }
        }
        expandedView = inflater.inflate(R.layout.island_expanded, null).also { view ->
            view.setOnClickListener { toggleExpanded() }
        }

        mediaSessionManager = MediaSessionManager(this)
        islandViewController = IslandViewController(expandedView!!, mediaSessionManager)

        mediaSessionManager.init(object : MediaSessionManager.Callback {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                updateCollapsedAlbumArt(metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART))
                islandViewController.updateMedia(metadata, mediaSessionManager.getPlaybackState())
            }
            override fun onPlaybackStateChanged(state: PlaybackState?) {
                val playing = state?.state == PlaybackState.STATE_PLAYING
                collapsedSoundWave?.setPlaying(playing)
                islandViewController.updateMedia(mediaSessionManager.getCurrentMetadata(), state)
            }
        })

        IslandNotificationListener.setCallback(object : IslandNotificationListener.NotificationCallback {
            override fun onNotificationPosted(data: IslandNotificationListener.NotificationData) {
                islandViewController.showNotification(data)
            }
            override fun onNotificationRemoved() {}
        })

        windowManager.addView(collapsedView, buildLayoutParams(collapsedWidth, collapsedHeight))
    }

    private fun updateCollapsedAlbumArt(bitmap: Bitmap?) {
        if (bitmap != null) {
            collapsedAlbumArt?.setImageBitmap(bitmap)
        } else {
            collapsedAlbumArt?.setImageResource(R.drawable.ic_play)
        }
    }

    private fun toggleExpanded() {
        val collapsed = collapsedView ?: return
        val expanded = expandedView ?: return
        if (!isExpanded) {
            windowManager.removeView(collapsed)
            windowManager.addView(expanded, buildLayoutParams(expandedWidth, expandedHeight))
            islandViewController.updateMedia(
                mediaSessionManager.getCurrentMetadata(),
                mediaSessionManager.getPlaybackState()
            )
            isExpanded = true
        } else {
            windowManager.removeView(expanded)
            windowManager.addView(collapsed, buildLayoutParams(collapsedWidth, collapsedHeight))
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
        val current = if (isExpanded) expandedView else collapsedView
        current?.let { if (it.isAttachedToWindow) windowManager.removeView(it) }
        mediaSessionManager.release()
        IslandNotificationListener.setCallback(null)
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
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Island overlay is active")
            .setSmallIcon(R.drawable.ic_play)
            .build()
    }

    companion object {
        private const val NOTIFICATION_ID = 1
        private const val CHANNEL_ID = "island_overlay_channel"
    }
}
