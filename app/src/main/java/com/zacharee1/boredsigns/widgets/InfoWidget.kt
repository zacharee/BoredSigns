package com.zacharee1.boredsigns.widgets

import android.Manifest
import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.wifi.SupplicantState
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.PowerManager
import android.preference.PreferenceManager
import android.provider.Settings
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.support.v4.app.NotificationManagerCompat
import android.telephony.*
import android.view.View
import android.widget.RemoteViews
import android.widget.Toast
import com.google.firebase.analytics.FirebaseAnalytics
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.services.InfoService
import com.zacharee1.boredsigns.util.Utils
import java.util.*

class InfoWidget : AppWidgetProvider() {
    companion object {
        const val MAX_NOTIF_COUNT = 10
    }

    private var mBatteryState: BatteryState = BatteryState(0, false)
    private var mMobileState: MobileSignalState = MobileSignalState(-1, false, "")
    private var mWiFiState: WiFiSignalState = WiFiSignalState(-1)

    private var mRankedNotifs = ArrayList<NotificationState>()

    private var mPrefs: SharedPreferences? = null

    private var mOldRanking: NotificationListenerService.RankingMap? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (perm in PermissionsActivity.INFO_REQUEST) {
            if (context.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(context, PermissionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("class", this::class.java)
                context.startActivity(intent)
                return
            }
        }

        val enabledListeners = NotificationManagerCompat.getEnabledListenerPackages(context)
        if (!enabledListeners.contains(context.packageName)) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Toast.makeText(context, context.resources.getText(R.string.grant_notification_access), Toast.LENGTH_LONG).show()
            return
        }

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

        val views = RemoteViews(context.packageName, R.layout.info_widget)

        views.setOnClickPendingIntent(R.id.clock, PendingIntent.getBroadcast(context, 0, Intent(InfoService.REFRESH), 0))

        try {
            updateBattery(views, context)
            updateClock(views)
            updateMobile(views, context, appWidgetManager, appWidgetIds)
            updateWifi(views, context)
            updateNotifications(views)
        } catch (e: Exception) {}

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
    }

    override fun onReceive(context: Context, intent: Intent?) {
        val rank = intent?.getBooleanExtra(InfoService.RANKING_LIST, false)

        rank?.let { if (it) InfoService.RANKING?.let {
                if (mOldRanking == null || !Arrays.equals(mOldRanking?.orderedKeys, it.orderedKeys)) {
                    mOldRanking = it
                    mRankedNotifs.clear()

                    for (ranking in it.orderedKeys) {
                        val index = it.orderedKeys.indexOf(ranking)

                        InfoService.NOTIFS?.let {notifs ->
                            if (index < notifs.size && index < MAX_NOTIF_COUNT) {
                                val rankInfo: NotificationListenerService.Ranking = NotificationListenerService.Ranking()
                                it.getRanking(ranking, rankInfo)

                                val notif = notifs[index]

                                val state = NotificationState(context, notif, rankInfo)

                                state.icon = notif?.notification?.smallIcon?.loadDrawable(context)

                                if (state.show) {
                                    mRankedNotifs.add(state)
                                }
                            }
                        }
                    }
                }
            }
        }

        if (intent?.action == Intent.ACTION_TIME_TICK) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isInteractive) {
                Utils.sendWidgetUpdate(context, this@InfoWidget::class.java, null)
            }
            return
        }

        super.onReceive(context, intent)
    }

    private fun updateBattery(views: RemoteViews, context: Context) {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1)
        val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, 0)
        val charging = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS

        var show = true
        var color = Color.WHITE
        var showPercent = true
        var showIcon = true

        mPrefs?.let {
            show = it.getBoolean("show_battery", true)
            color = it.getInt("battery_color", Color.WHITE)
            showPercent = it.getBoolean("show_percent", true)
            showIcon = it.getBoolean("show_batt_icon", true)
        }

        if (show) {
            mBatteryState.updateState(level, charging)

            views.setViewVisibility(R.id.battery, View.VISIBLE)

            if (showIcon) {
                views.setViewVisibility(R.id.battery_view, View.VISIBLE)
                views.setImageViewResource(R.id.battery_view, mBatteryState.imageResource)
                views.setInt(R.id.battery_view, "setColorFilter", color)
            } else {
                views.setViewVisibility(R.id.battery_view, View.GONE)
            }

            if (showPercent) {
                views.setViewVisibility(R.id.battery_percent, View.VISIBLE)
                views.setTextViewText(R.id.battery_percent, mBatteryState.percent.toString() + "%")
                views.setTextColor(R.id.battery_percent, color)
            } else {
                views.setViewVisibility(R.id.battery_percent, View.GONE)
            }
        } else {
            views.setViewVisibility(R.id.battery, View.GONE)
        }
    }

    private fun updateClock(views: RemoteViews) {
        var show = true
        var hour_24 = false
        var amPm = true
        var showDate = false
        var color = Color.WHITE

        mPrefs?.let {
            show = it.getBoolean("show_clock", true)
            hour_24 = it.getBoolean("24_hour", false)
            amPm = it.getBoolean("am_pm", true)
            showDate = it.getBoolean("show_date", false)
            color = it.getInt("clock_color", Color.WHITE)
        }

        if (show) {
            views.setViewVisibility(R.id.clock, View.VISIBLE)

            val format: CharSequence = if (showDate) "EE, d " else {""} +  if (hour_24) "k" else {"h"} + ":mm" + if (amPm) " a" else {""}

            views.setCharSequence(R.id.textClock, "setFormat12Hour", format)
            views.setCharSequence(R.id.textClock, "setFormat24Hour", format)

            views.setTextColor(R.id.textClock, color)
        } else {
            views.setViewVisibility(R.id.clock, View.GONE)
        }
    }

    private fun updateMobile(views: RemoteViews, context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        var show = true
        var color = Color.WHITE

        mPrefs?.let {
            show = it.getBoolean("show_mobile", true)
            color = it.getInt("mobile_color", Color.WHITE)
        }

        if (show && context.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val telephony = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            var level = 0
            var connected = false
            var text = getNetworkTypeString(context)
            val airplane = Settings.Global.getInt(context.contentResolver, Settings.Global.AIRPLANE_MODE_ON, 0) != 0

            val hasSim = telephony.simState != TelephonyManager.SIM_STATE_ABSENT

            val info = telephony.allCellInfo[0]
            if (info is CellInfoGsm) {
                level = info.cellSignalStrength.level
                connected = info.isRegistered
            }
            if (info is CellInfoCdma) {
                level = info.cellSignalStrength.level
                connected = info.isRegistered
            }
            if (info is CellInfoWcdma) {
                level = info.cellSignalStrength.level
                connected = info.isRegistered
            }
            if (info is CellInfoLte) {
                level = info.cellSignalStrength.level
                connected = info.isRegistered
            }

            telephony.listen(object : PhoneStateListener() {
                override fun onServiceStateChanged(serviceState: ServiceState?) {
                    val hasService = serviceState?.state == ServiceState.STATE_IN_SERVICE

                    if (airplane) level = -3
                    else if (!hasSim) level = -2
                    else if (!hasService) level = -1

                    mMobileState.updateState(level, connected, text)
                    views.setImageViewResource(R.id.mobile_view, mMobileState.imageResource)
                    views.setTextViewText(R.id.mobile_text, mMobileState.type)

                    appWidgetManager.updateAppWidget(appWidgetIds, views)
                }
            }, PhoneStateListener.LISTEN_SERVICE_STATE)

            views.setViewVisibility(R.id.mobile, View.VISIBLE)
            views.setInt(R.id.mobile_view, "setColorFilter", color)
            views.setTextColor(R.id.mobile_text, color)
        } else {
            views.setViewVisibility(R.id.mobile, View.GONE)
        }
    }

    private fun updateWifi(views: RemoteViews, context: Context) {
        var show = true
        var color = Color.WHITE

        mPrefs?.let {
            show = it.getBoolean("show_wifi", true)
            color = it.getInt("wifi_color", Color.WHITE)
        }

        val wifiMan = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        show = show && wifiMan.isWifiEnabled && wifiMan.connectionInfo.supplicantState != SupplicantState.DISCONNECTED

        if (show) {
            val level = WifiManager.calculateSignalLevel(wifiMan.connectionInfo.rssi, 5)

            views.setViewVisibility(R.id.wifi, View.VISIBLE)
            mWiFiState.updateState(level)

            views.setImageViewResource(R.id.wifi_view, mWiFiState.imageResource)
            views.setInt(R.id.wifi_view, "setColorFilter", color)
        } else {
            views.setViewVisibility(R.id.wifi, View.GONE)
        }
    }

    private fun updateNotifications(views: RemoteViews) {
        var show = true

        mPrefs?.let {
            show = it.getBoolean("show_notifs", true)
        }

        if (show) {
            views.setViewVisibility(R.id.notifications_view, View.VISIBLE)
            var limit = 6
            if (mRankedNotifs.size - 1 < limit) limit = mRankedNotifs.size - 1

            for (i in 0..6) {
                var id = 0

                when (i) {
                    0 -> id = R.id.notif_icon_0
                    1 -> id = R.id.notif_icon_1
                    2 -> id = R.id.notif_icon_2
                    3 -> id = R.id.notif_icon_3
                    4 -> id = R.id.notif_icon_4
                    5 -> id = R.id.notif_icon_5
                    6 -> id = R.id.notif_icon_6
                }

                views.setImageViewBitmap(id, null)

                if (i <= limit) {
                    val notif = mRankedNotifs[i]
                    if (notif.show) {
                        views.setImageViewBitmap(id, drawableToBitmap(notif.icon))
                    }
                }
            }
        } else {
            views.setViewVisibility(R.id.notifications_view, View.GONE)
        }
    }

    private fun drawableToBitmap(drawable: Drawable?): Bitmap? {
        var bitmap: Bitmap? = null

        drawable?.let {
            if (it is BitmapDrawable) {
                return it.bitmap
            }

            if (it.intrinsicWidth <= 0 || it.intrinsicHeight <= 0) {
                bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            } else {
                bitmap = Bitmap.createBitmap(it.intrinsicWidth, it.intrinsicHeight, Bitmap.Config.ARGB_8888)
            }

            val canvas = Canvas(bitmap)
            it.bounds = Rect(0, 0, canvas.width, canvas.height)
            it.draw(canvas)
        }

        return bitmap
    }

    private fun getNetworkTypeString(context: Context): String {
        val telMan = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager

        when (telMan.networkType) {
            TelephonyManager.NETWORK_TYPE_1xRTT -> return "1x"
            TelephonyManager.NETWORK_TYPE_CDMA -> return "IS95"
            TelephonyManager.NETWORK_TYPE_EDGE -> return "E"
            TelephonyManager.NETWORK_TYPE_EHRPD -> return "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_0 -> return "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_A -> return "3G"
            TelephonyManager.NETWORK_TYPE_EVDO_B -> return "3G"
            TelephonyManager.NETWORK_TYPE_GPRS -> return "G"
            TelephonyManager.NETWORK_TYPE_GSM -> return "2G"
            TelephonyManager.NETWORK_TYPE_HSDPA -> return "H"
            TelephonyManager.NETWORK_TYPE_HSPA -> return "H"
            TelephonyManager.NETWORK_TYPE_HSPAP -> return "H+"
            TelephonyManager.NETWORK_TYPE_HSUPA -> return "H"
            TelephonyManager.NETWORK_TYPE_IDEN -> return "IDEN"
            TelephonyManager.NETWORK_TYPE_IWLAN -> return "IWLAN"
            TelephonyManager.NETWORK_TYPE_LTE -> return "LTE"
            TelephonyManager.NETWORK_TYPE_TD_SCDMA -> return "3G"
            TelephonyManager.NETWORK_TYPE_UMTS -> return "3G"
            TelephonyManager.NETWORK_TYPE_UNKNOWN -> return ""
            else -> return ""
        }
    }

    class BatteryState(percentage: Int, charging: Boolean) {
        var percent = 0
        var isCharging = false
        var imageResource = R.drawable.ic_battery_alert_black_24dp

        init {
            updateState(percentage, charging)
        }

        fun updateState(percent: Int, charging: Boolean) {
            this.percent = percent
            isCharging = charging

            if (percent >= 95) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_full_black_24dp else R.drawable.ic_battery_full_black_24dp
            } else if (percent >= 85) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_90_black_24dp else R.drawable.ic_battery_90_black_24dp
            } else if (percent >= 70) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_80_black_24dp else R.drawable.ic_battery_80_black_24dp
            } else if (percent >= 55) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_60_black_24dp else R.drawable.ic_battery_60_black_24dp
            } else if (percent >= 40) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_50_black_24dp else R.drawable.ic_battery_50_black_24dp
            } else if (percent >= 25) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_30_black_24dp else R.drawable.ic_battery_30_black_24dp
            } else if (percent >= 10) {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_20_black_24dp else R.drawable.ic_battery_20_black_24dp
            } else {
                imageResource = if (isCharging) R.drawable.ic_battery_charging_20_black_24dp else R.drawable.ic_battery_alert_black_24dp
            }
        }
    }

    class MobileSignalState(level: Int, connected: Boolean, type: String) {
        var level = -1
        var connected = false
        var imageResource = R.drawable.ic_signal_cellular_null_black_24dp
        var type = ""

        init {
            updateState(level, connected, type)
        }

        fun updateState(level: Int, connected: Boolean, type: String) {
            this.level = level
            this.connected = connected
            this.type = type

            imageResource = when (level) {
                -3 -> R.drawable.ic_airplanemode_active_black_24dp
                -2 -> R.drawable.ic_signal_cellular_no_sim_black_24dp
                -1 -> R.drawable.ic_signal_cellular_null_black_24dp
                0 -> if (connected) R.drawable.ic_signal_cellular_0_bar_black_24dp else R.drawable.ic_signal_cellular_connected_no_internet_0_bar_black_24dp
                1 -> if (connected) R.drawable.ic_signal_cellular_1_bar_black_24dp else R.drawable.ic_signal_cellular_connected_no_internet_1_bar_black_24dp
                2 -> if (connected) R.drawable.ic_signal_cellular_2_bar_black_24dp else R.drawable.ic_signal_cellular_connected_no_internet_2_bar_black_24dp
                3 -> if (connected) R.drawable.ic_signal_cellular_3_bar_black_24dp else R.drawable.ic_signal_cellular_connected_no_internet_3_bar_black_24dp
                4 -> if (connected) R.drawable.ic_signal_cellular_4_bar_black_24dp else R.drawable.ic_signal_cellular_connected_no_internet_4_bar_black_24dp
                else -> R.drawable.ic_signal_cellular_null_black_24dp
            }
        }
    }

    class WiFiSignalState(level: Int) {
        var level = -1
        var imageResource = R.drawable.ic_signal_wifi_null_black_24dp

        init {
            updateState(level)
        }

        fun updateState(level: Int) {
            this.level = level

            imageResource = when (level) {
                0 -> R.drawable.ic_signal_wifi_0_bar_black_24dp
                1 -> R.drawable.ic_signal_wifi_1_bar_black_24dp
                2 -> R.drawable.ic_signal_wifi_2_bar_black_24dp
                3 -> R.drawable.ic_signal_wifi_3_bar_black_24dp
                4 -> R.drawable.ic_signal_wifi_4_bar_black_24dp
                else -> R.drawable.ic_signal_wifi_null_black_24dp
            }
        }
    }

    class NotificationState(context: Context, private val notification: StatusBarNotification?, val info: NotificationListenerService.Ranking) {
        var show: Boolean = info.importance > NotificationManager.IMPORTANCE_MIN
        var icon: Drawable? = context.resources?.getDrawable(R.drawable.ic_android_black_24dp, null)

        init {
            try {
                icon = notification?.notification?.smallIcon?.loadDrawable(context)
            } catch (e: PackageManager.NameNotFoundException) {
                val bundle = Bundle()
                bundle.putString("message", e.localizedMessage)
                bundle.putString("stacktrace", Arrays.toString(e.stackTrace))
                bundle.putString("package", notification?.packageName)
                FirebaseAnalytics.getInstance(context).logEvent("info_widget_package_error", bundle)
            }
        }

        override fun equals(other: Any?): Boolean {
            if (other is NotificationState) {
                return notification?.packageName == other.notification?.packageName
            }

            return false
        }

        override fun hashCode(): Int {
            return info.importance.hashCode() + show.hashCode() + if (icon == null) 0 else (icon as Drawable).hashCode()
        }

        override fun toString(): String {
            return "[ level: " + info.importance + " show: " + show + " package: " + notification?.packageName + " ]"
        }
    }
}

