package com.example.maps_api

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Color
import androidx.core.app.NotificationCompat

class NotificationHelper (base: Context) : ContextWrapper(base){
    private var notifManager: NotificationManager? = null
    private val manager: NotificationManager?
        get() {
            if(notifManager == null) {
                notifManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            }
            return notifManager
        }

    init {
        createChannels()
    }

    private fun createChannels() {
        val notificationChannel = NotificationChannel(CHANNEL_ONE_ID, CHANNEL_ONE_NAME, NotificationManager.IMPORTANCE_HIGH)
        notificationChannel.enableLights(true)
        notificationChannel.lightColor = Color.BLUE
        notificationChannel.setShowBadge(true)
        notificationChannel.lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        manager!!.createNotificationChannel(notificationChannel)
    }

    fun getNotification(title: String, body: String): NotificationCompat.Builder {
        val intent = Intent(this, MapsActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent: PendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)

        return NotificationCompat.Builder(applicationContext, CHANNEL_ONE_ID)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(R.drawable.notifications_icon)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
    }

    fun notify(id: Int, notification: NotificationCompat.Builder) {
        manager!!.notify(id, notification.build())
    }

    companion object {
        const val CHANNEL_ONE_ID = "com.example.tomowen.notificatonsexample.ONE"
        const val CHANNEL_ONE_NAME = "Channel One"
    }
}