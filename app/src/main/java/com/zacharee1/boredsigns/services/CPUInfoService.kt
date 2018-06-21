package com.zacharee1.boredsigns.services

import android.app.Service
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.CPUInfoWidget


class CPUInfoService : Service() {
    private var isRunning = false

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        isRunning = true
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

        isRunning = false
    }
}
