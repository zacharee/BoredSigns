package com.zacharee1.boredsigns

import android.app.Notification
import android.app.Service
import android.content.*
import android.os.IBinder
import android.preference.PreferenceManager
import com.zacharee1.boredsigns.widgets.InfoWidget
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.content.ComponentName
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Binder
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import android.util.Log
import java.util.*


class InfoService : NotificationListenerService() {
    companion object {
        var NOTIFS: Array<StatusBarNotification?>? = null
        var RANKING: RankingMap? = null

        val NOTIF_BASE = "com.zacharee1.boredsigns.action."
        val NOTIF_UPDATE = NOTIF_BASE + "NOTIF_UPDATE"

        val NOTIF_LIST = "notifs"
        val RANKING_LIST = "ranks"

        var KEYS = mutableListOf(
                "show_percent",
                "battery_color",
                "am_pm",
                "24_hour",
                "clock_color",
                "show_date",
                "show_battery",
                "show_clock",
                "show_mobile",
                "mobile_color",
                "show_wifi",
                "wifi_color",
                "show_notifs"
        )

        var INTENTS = mutableListOf(
                Intent.ACTION_BATTERY_CHANGED,
                Intent.ACTION_POWER_CONNECTED,
                Intent.ACTION_POWER_DISCONNECTED,
                WifiManager.NETWORK_STATE_CHANGED_ACTION,
                WifiManager.RSSI_CHANGED_ACTION,
                WifiManager.WIFI_STATE_CHANGED_ACTION,
                WifiManager.SUPPLICANT_CONNECTION_CHANGE_ACTION,
                WifiManager.SUPPLICANT_STATE_CHANGED_ACTION,
                ConnectivityManager.CONNECTIVITY_ACTION,
                NOTIF_UPDATE
        )
    }

    private var mConnected = false

    private var mPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = SharedPreferences.OnSharedPreferenceChangeListener {
        _, s ->

        if (KEYS.contains(s)) {
            sendUpdateBroadcast(null)
        }
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (INTENTS.contains(p1?.action)) {
                sendUpdateBroadcast(p1?.extras)
            }
        }
    }

    private var mTelephonyListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            sendUpdateBroadcast(null)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        val extras = Bundle()
        sendUpdateBroadcast(extras)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        val extras = Bundle()
        sendUpdateBroadcast(extras)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        val extras = Bundle()
        sendUpdateBroadcast(extras)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(1337, Notification())

        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        val filter = IntentFilter()
        for (s in INTENTS) {
            filter.addAction(s)
        }

        registerReceiver(mReceiver, filter)
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mPrefsListener)

        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).listen(mTelephonyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        sendUpdateBroadcast(null)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        mConnected = true

        RANKING = currentRanking
        NOTIFS = activeNotifications

        val extras = Bundle()
        extras.putBoolean(RANKING_LIST, true)
        extras.putBoolean(NOTIF_LIST, true)

        sendUpdateBroadcast(extras)
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()

        mConnected = false
    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(mReceiver)
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(mPrefsListener)
    }

    private fun sendUpdateBroadcast(extras: Bundle?) {
        if (mConnected) {
            RANKING = currentRanking
            NOTIFS = activeNotifications
        }

        val man = AppWidgetManager.getInstance(this)
        val ids = man.getAppWidgetIds(
                ComponentName(this, InfoWidget::class.java))
        val updateIntent = Intent()
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra("appWidgetIds", ids)
        if (mConnected) updateIntent.putExtra(RANKING_LIST, true)
        if (mConnected) updateIntent.putExtra(NOTIF_LIST, true)
        if (extras != null) updateIntent.putExtras(extras)
        updateIntent.component = ComponentName(this, InfoWidget::class.java)
        sendBroadcast(updateIntent)
    }
}
