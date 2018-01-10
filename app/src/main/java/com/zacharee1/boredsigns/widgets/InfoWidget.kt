package com.zacharee1.boredsigns.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.*
import android.graphics.Color
import android.opengl.Visibility
import android.os.BatteryManager
import android.preference.PreferenceManager
import android.util.Log
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.RemoteViews
import com.zacharee1.boredsigns.InfoService

import com.zacharee1.boredsigns.R

class InfoWidget : AppWidgetProvider() {
    private var mBatteryManager: BatteryManager? = null
    private var mBatteryState: BatteryState = BatteryState(0, false)

    private var mPrefs: SharedPreferences? = null

    private var mServiceIntent: Intent? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        setServiceIntent(context)

        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)
        mBatteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

        context.startService(mServiceIntent)

        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    override fun onEnabled(context: Context) {
        setServiceIntent(context)

        context.startService(mServiceIntent)
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)

        setServiceIntent(context)
        context?.startService(mServiceIntent)
        mPrefs = PreferenceManager.getDefaultSharedPreferences(context)

    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)

        setServiceIntent(context)
        context?.stopService(mServiceIntent)
    }

    override fun onDisabled(context: Context) {
        setServiceIntent(context)
        context.stopService(mServiceIntent)
    }

    private fun setServiceIntent(context: Context?) {
        mServiceIntent = Intent(context, InfoService::class.java)
    }

    private fun updateAppWidget(context: Context, appWidgetManager: AppWidgetManager,
                                appWidgetId: Int) {

        // Construct the RemoteViews object
        val views = RemoteViews(context.packageName, R.layout.info_widget)

        updateClock(views)
        updateBattery(views)

        // Instruct the widget manager to update the widget
        appWidgetManager.updateAppWidget(appWidgetId, views)
    }

    private fun updateBattery(views: RemoteViews) {
        val level = mBatteryManager?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY)
        val charging = mBatteryManager?.isCharging

        mBatteryState.updateState(level as Int, charging as Boolean)

        views.setImageViewResource(R.id.battery_view, mBatteryState.imageResource)

        var color = Color.WHITE
        var showPercent = true

        mPrefs?.let {
            color = it.getInt("battery_color", Color.WHITE)
            showPercent = it.getBoolean("show_percent", true)
        }

        views.setInt(R.id.battery_view, "setColorFilter", color)
        views.setTextColor(R.id.battery_percent, color)

        if (showPercent) {
            views.setViewVisibility(R.id.battery_percent, View.VISIBLE)
            views.setTextViewText(R.id.battery_percent, mBatteryState.percent.toString() + "%")
        } else {
            views.setViewVisibility(R.id.battery_percent, View.GONE)
        }
    }

    private fun updateClock(views: RemoteViews) {
        var hour_24 = false
        var amPm = true
        var showDate = false
        var color = Color.WHITE

        mPrefs?.let {
            hour_24 = it.getBoolean("24_hour", false)
            amPm = it.getBoolean("am_pm", true)
            showDate = it.getBoolean("show_date", false)
            color = it.getInt("clock_color", Color.WHITE)
        }

        val format: CharSequence = if (showDate) "EE, d " else {""} +  if (hour_24) "k" else {"h"} + ":mm" + if (amPm) " a" else {""}

        views.setCharSequence(R.id.clock_view, "setFormat12Hour", format)
        views.setCharSequence(R.id.clock_view, "setFormat24Hour", format)

        views.setTextColor(R.id.clock_view, color)
    }

    class BatteryState(percentage: Int, charging: Boolean) {
        var percent: Int = 0
        var isCharging: Boolean = false
        var imageResource: Int = R.drawable.ic_battery_alert_black_24dp

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
}

