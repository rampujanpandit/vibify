package com.vibify.music

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build

class VibifyApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                PLAYBACK_CHANNEL_ID,
                "Vibify Music Playback",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows currently playing music controls"
                setShowBadge(false)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    companion object {
        const val PLAYBACK_CHANNEL_ID = "vibify_playback_channel"
        const val PLAYBACK_NOTIFICATION_ID = 1001
    }
}
