package com.vibify.music.player

import android.app.PendingIntent
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.vibify.music.MainActivity
import com.vibify.music.R
import com.vibify.music.VibifyApplication

/**
 * Foreground media service. startForeground() runs immediately so Android
 * does not kill the app, then Media3 replaces the notification with playback controls.
 */
class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(VibifyApplication.PLAYBACK_CHANNEL_ID)
                .setNotificationId(VibifyApplication.PLAYBACK_NOTIFICATION_ID)
                .build()
        )
        super.onCreate()
        enterForeground()

        val player = VibifyAudioPlayer.getInstance(this).exoPlayerInternal
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        mediaSession = MediaSession.Builder(this, player)
            .setId("vibify_playback")
            .setSessionActivity(openApp)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enterForeground()
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    override fun onDestroy() {
        mediaSession?.release()
        mediaSession = null
        super.onDestroy()
    }

    private fun enterForeground() {
        val openApp = PendingIntent.getActivity(
            this,
            1,
            Intent(this, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(this, VibifyApplication.PLAYBACK_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_play_notification)
            .setContentTitle("Vibify")
            .setContentText("Playing")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(openApp)
            .setCategory(NotificationCompat.CATEGORY_TRANSPORT)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
        try {
            ServiceCompat.startForeground(
                this,
                VibifyApplication.PLAYBACK_NOTIFICATION_ID,
                notification,
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
                } else {
                    0
                }
            )
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
        }
    }

    companion object {
        private const val TAG = "PlaybackService"
    }
}
