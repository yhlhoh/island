package com.island.app.media

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import com.island.app.service.IslandNotificationListener

class MediaSessionManager(private val context: Context) {

    interface Callback {
        fun onMetadataChanged(metadata: MediaMetadata?)
        fun onPlaybackStateChanged(state: PlaybackState?)
    }

    private val systemMediaSessionManager: MediaSessionManager =
        context.getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager

    private var activeController: MediaController? = null
    private var userCallback: Callback? = null

    private val controllerCallback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) {
            userCallback?.onMetadataChanged(metadata)
        }
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            userCallback?.onPlaybackStateChanged(state)
        }
    }

    private val sessionsChangedListener =
        MediaSessionManager.OnActiveSessionsChangedListener { controllers ->
            updateActiveController(controllers)
        }

    fun init(callback: Callback) {
        userCallback = callback
        val notifListenerComponent = ComponentName(context, IslandNotificationListener::class.java)
        try {
            val controllers = systemMediaSessionManager.getActiveSessions(notifListenerComponent)
            updateActiveController(controllers)
            systemMediaSessionManager.addOnActiveSessionsChangedListener(
                sessionsChangedListener, notifListenerComponent
            )
        } catch (e: SecurityException) {
            // Notification listener permission not granted yet
        }
    }

    private fun updateActiveController(controllers: List<MediaController>?) {
        activeController?.unregisterCallback(controllerCallback)
        activeController = controllers?.firstOrNull()
        activeController?.registerCallback(controllerCallback)
        userCallback?.onMetadataChanged(activeController?.metadata)
        userCallback?.onPlaybackStateChanged(activeController?.playbackState)
    }

    fun play() { activeController?.transportControls?.play() }
    fun pause() { activeController?.transportControls?.pause() }
    fun skipNext() { activeController?.transportControls?.skipToNext() }
    fun skipPrev() { activeController?.transportControls?.skipToPrevious() }

    fun getCurrentMetadata(): MediaMetadata? = activeController?.metadata
    fun getPlaybackState(): PlaybackState? = activeController?.playbackState

    fun release() {
        try {
            systemMediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        } catch (e: Exception) { /* ignore */ }
        activeController?.unregisterCallback(controllerCallback)
        activeController = null
    }
}
