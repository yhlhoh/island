package com.island.app.view

import android.media.MediaMetadata
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import com.island.app.R
import com.island.app.media.MediaSessionManager
import com.island.app.service.IslandNotificationListener

class IslandViewController(
    private val expandedView: View,
    private val mediaSessionManager: MediaSessionManager
) {
    private val mediaCard: View = expandedView.findViewById(R.id.mediaCard)
    private val notificationCard: View = expandedView.findViewById(R.id.notificationCard)

    private val tvTitle: TextView = expandedView.findViewById(R.id.tvTitle)
    private val tvArtist: TextView = expandedView.findViewById(R.id.tvArtist)
    private val ivAlbumArt: ImageView = expandedView.findViewById(R.id.ivAlbumArt)
    private val btnPlayPause: ImageButton = expandedView.findViewById(R.id.btnPlayPause)
    private val btnPrev: ImageButton = expandedView.findViewById(R.id.btnPrev)
    private val btnNext: ImageButton = expandedView.findViewById(R.id.btnNext)
    private val soundWave: SoundWaveView = expandedView.findViewById(R.id.soundWave)

    private val tvNotifTitle: TextView = expandedView.findViewById(R.id.tvNotifTitle)
    private val tvNotifText: TextView = expandedView.findViewById(R.id.tvNotifText)
    private val ivNotifIcon: ImageView = expandedView.findViewById(R.id.ivNotifIcon)

    private val handler = Handler(Looper.getMainLooper())
    private val revertToMediaRunnable = Runnable { showMedia() }

    init {
        btnPlayPause.setOnClickListener {
            val state = mediaSessionManager.getPlaybackState()
            if (state?.state == PlaybackState.STATE_PLAYING) {
                mediaSessionManager.pause()
            } else {
                mediaSessionManager.play()
            }
        }
        btnPrev.setOnClickListener { mediaSessionManager.skipPrev() }
        btnNext.setOnClickListener { mediaSessionManager.skipNext() }
        showMedia()
    }

    fun showMedia() {
        handler.removeCallbacks(revertToMediaRunnable)
        mediaCard.visibility = View.VISIBLE
        notificationCard.visibility = View.GONE
    }

    fun showNotification(data: IslandNotificationListener.NotificationData) {
        tvNotifTitle.text = data.title
        tvNotifText.text = data.text
        if (data.icon != null) {
            ivNotifIcon.setImageIcon(data.icon)
            ivNotifIcon.visibility = View.VISIBLE
        } else {
            ivNotifIcon.visibility = View.GONE
        }
        mediaCard.visibility = View.GONE
        notificationCard.visibility = View.VISIBLE
        handler.removeCallbacks(revertToMediaRunnable)
        handler.postDelayed(revertToMediaRunnable, 4000)
    }

    fun updateMedia(metadata: MediaMetadata?, playbackState: PlaybackState?) {
        val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE) ?: ""
        val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST) ?: ""
        tvTitle.text = title
        tvArtist.text = artist

        val albumArt = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        if (albumArt != null) {
            ivAlbumArt.setImageBitmap(albumArt)
        } else {
            ivAlbumArt.setImageResource(R.drawable.ic_play)
        }

        val isPlaying = playbackState?.state == PlaybackState.STATE_PLAYING
        btnPlayPause.setImageResource(
            if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
        )
        soundWave.setPlaying(isPlaying)
    }
}
