package com.zacharee1.boredsigns.services

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.*
import android.net.ConnectivityManager
import android.net.wifi.WifiManager
import android.os.Build
import android.os.Bundle
import android.preference.PreferenceManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationCompat
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyManager
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.InfoWidget


class InfoService : NotificationListenerService() {
    companion object {
        var NOTIFS: Array<StatusBarNotification?>? = null
        var RANKING: RankingMap? = null

        const val BASE = "com.zacharee1.boredsigns.action."
        const val NOTIF_UPDATE = BASE + "NOTIF_UPDATE"
        const val REFRESH = BASE + "REFRESH"

        const val NOTIF_LIST = "notifs"
        const val RANKING_LIST = "ranks"

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
                "show_notifs",
                "show_batt_icon"
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
                NOTIF_UPDATE,
                REFRESH
        )
    }

    private var mConnected = false

    private var mPrefsListener: SharedPreferences.OnSharedPreferenceChangeListener? = SharedPreferences.OnSharedPreferenceChangeListener {
        _, s ->

        if (KEYS.contains(s) && shouldAct()) {
            sendUpdateBroadcast(null)
        }
    }

    private var mReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            if (INTENTS.contains(p1?.action) && shouldAct()) {
                sendUpdateBroadcast(p1?.extras)
            }
        }
    }

    private var mTelephonyListener: PhoneStateListener = object : PhoneStateListener() {
        override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
            if (shouldAct()) sendUpdateBroadcast(null)
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        if (shouldAct()) sendUpdateBroadcast(null)
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap?) {
        if (shouldAct()) sendUpdateBroadcast(null)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification?) {
        if (shouldAct()) sendUpdateBroadcast(null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(NotificationChannel("info", resources.getText(R.string.info_widget_title), NotificationManager.IMPORTANCE_LOW))
        }

        startForeground(1337,
                NotificationCompat.Builder(this, "info")
                        .setSmallIcon(R.mipmap.ic_launcher_boredsigns)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build())

        if (!shouldAct()) {
            stopForeground(true)
            stopSelf()
            stopSelf(1337)
        }

        val filter = IntentFilter()
        for (s in INTENTS) {
            filter.addAction(s)
        }

        registerReceiver(mReceiver, filter)
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(mPrefsListener)

        (getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).listen(mTelephonyListener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)

        if (shouldAct()) sendUpdateBroadcast(null)
    }

    override fun onListenerConnected() {
        super.onListenerConnected()

        mConnected = true

        if (shouldAct()) {
            sendUpdateBroadcast(null)
        }
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

    private fun shouldAct(): Boolean {
        return Utils.isWidgetInUse(InfoWidget::class.java, this)
    }

    private fun sendUpdateBroadcast(extras: Bundle?) {
        val update = extras ?: Bundle()

        if (mConnected) {
            RANKING = currentRanking
            NOTIFS = activeNotifications

            update.putBoolean(RANKING_LIST, true)
            update.putBoolean(NOTIF_LIST, true)
        }

        Utils.sendWidgetUpdate(this, InfoWidget::class.java, update)
    }
}
