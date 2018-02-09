package com.zacharee1.boredsigns.services

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.support.v4.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.zacharee1.boredsigns.R

class FirebaseService : FirebaseMessagingService() {
    override fun onMessageReceived(p0: RemoteMessage?) {
        super.onMessageReceived(p0)

        val message = p0?.notification?.body

        val notification = NotificationCompat.Builder(this, "boredsigns")
                .setSmallIcon(R.mipmap.ic_launcher_boredsigns)
                .setContentTitle(resources.getString(R.string.app_name))
                .setContentText(message)

        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(0, notification.build())
    }
}