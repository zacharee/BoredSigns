package com.zacharee1.boredsigns.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.app.NotificationCompat
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.CPUInfoWidget


class CPUInfoService : Service() {
    private var isRunning = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        isRunning = true
        startForeground()
        startListening()
        super.onCreate()
    }

    private fun startListening() {
        Utils.sendWidgetUpdate(this, CPUInfoWidget::class.java, null)

        if (isRunning) {
            Handler(Looper.getMainLooper()).postDelayed({ startListening() }, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        isRunning = false
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(NotificationChannel("cpu",
                    resources.getString(R.string.cpu_widget_title), NotificationManager.IMPORTANCE_LOW))
        }

        startForeground(1337,
                NotificationCompat.Builder(this, "cpu")
                        .setSmallIcon(R.mipmap.ic_launcher_boredsigns)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build())
    }
}
