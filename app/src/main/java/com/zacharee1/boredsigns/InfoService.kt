package com.zacharee1.boredsigns

import android.app.Service
import android.content.*
import android.os.BatteryManager
import android.os.IBinder
import android.preference.PreferenceManager
import android.util.Log
import com.zacharee1.boredsigns.widgets.InfoWidget
import android.R.attr.data
import android.appwidget.AppWidgetManager
import android.support.v4.view.accessibility.AccessibilityEventCompat.setAction
import android.content.Intent
import android.content.ComponentName



class InfoService : Service() {
    companion object {
        var KEYS = mutableListOf(
                "show_percent",
                "battery_color",
                "am_pm",
                "24_hour",
                "clock_color",
                "show_date"
        )
    }

    private var mPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = SharedPreferences.OnSharedPreferenceChangeListener {
        _, s ->

        if (KEYS.contains(s)) {
            sendUpdateBroadcast()
        }
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                Intent.ACTION_BATTERY_CHANGED -> sendUpdateBroadcast()
                Intent.ACTION_POWER_CONNECTED -> sendUpdateBroadcast()
                Intent.ACTION_POWER_DISCONNECTED -> sendUpdateBroadcast()
            }
        }
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_BATTERY_CHANGED)
        filter.addAction(Intent.ACTION_POWER_DISCONNECTED)
        filter.addAction(Intent.ACTION_POWER_CONNECTED)

        registerReceiver(mReceiver, filter)
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mPrefsListener)
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mReceiver)
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mPrefsListener)
    }

    private fun sendUpdateBroadcast() {
        val man = AppWidgetManager.getInstance(this)
        val ids = man.getAppWidgetIds(
                ComponentName(this, InfoWidget::class.java))
        val updateIntent = Intent()
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra("appWidgetIds", ids)
        updateIntent.component = ComponentName(this, InfoWidget::class.java)
        sendBroadcast(updateIntent)
    }
}
