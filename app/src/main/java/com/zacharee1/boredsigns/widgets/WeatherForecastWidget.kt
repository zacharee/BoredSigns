package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.RemoteViews
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.services.WeatherService
import com.zacharee1.boredsigns.util.Utils

class WeatherForecastWidget : AppWidgetProvider() {
    private var tempHigh: ArrayList<String>? = null
    private var tempLow: ArrayList<String>? = null
    private var loc: String? = null
    private var icon: ArrayList<String>? = null
    private var times: ArrayList<String>? = null

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (perm in PermissionsActivity.WEATHER_REQUEST) {
            if (context.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(context, PermissionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("class", this::class.java)
                context.startActivity(intent)
                return
            }
        }

        startService(context)

        val views = RemoteViews(context.packageName, R.layout.weather_forecast_widget)

        val intent = Intent(context, WeatherService::class.java)
        intent.action = WeatherService.ACTION_UPDATE_WEATHER
        val pIntent = PendingIntent.getService(context, 10, intent, 0)
        views.setOnClickPendingIntent(R.id.refresh, pIntent)

        views.setViewVisibility(R.id.refresh, View.GONE)
        views.setViewVisibility(R.id.loading, View.VISIBLE)
        setYahooPendingIntent(views, context)
        setThings(views, context)

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        intent?.let {
            tempHigh = it.getStringArrayListExtra(WeatherService.EXTRA_TEMP)
            loc = it.getStringExtra(WeatherService.EXTRA_LOC)
            icon = it.getStringArrayListExtra(WeatherService.EXTRA_ICON)
            times = it.getStringArrayListExtra(WeatherService.EXTRA_TIME)
            tempLow = it.getStringArrayListExtra(WeatherService.EXTRA_TEMP_EX)
        }

        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context) {
        super.onEnabled(context)
        startService(context)
    }

    override fun onRestored(context: Context, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        startService(context)
    }

    override fun onDeleted(context: Context, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        stopService(context)
    }

    override fun onDisabled(context: Context) {
        super.onDisabled(context)
        stopService(context)
    }

    private fun setYahooPendingIntent(views: RemoteViews, context: Context) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = Uri.parse("https://openweathermap.org/")
        val pendingIntent = PendingIntent.getActivity(context, 1337, intent, 0)

        views.setOnClickPendingIntent(R.id.owm, pendingIntent)
    }

    private fun setThings(views: RemoteViews, context: Context) {
        if (loc == null || tempHigh == null || times == null || tempLow == null) {
            sendUpdate(context)
        } else {
            views.setViewVisibility(R.id.loading, View.GONE)
            views.setViewVisibility(R.id.refresh, View.VISIBLE)

            views.removeAllViews(R.id.weather_block_wrap)

            for (i in 0 until (tempLow?.size ?: 5)) {
                val view = RemoteViews(context.packageName, R.layout.weather_forecast_block)
                if (icon != null) view.setImageViewBitmap(R.id.icon, Utils.processBmp(icon?.get(i), context))
                view.setTextViewText(R.id.tempHigh, tempHigh?.get(i))
                view.setTextViewText(R.id.tempLow, tempLow?.get(i))

                views.addView(R.id.weather_block_wrap, view)
            }

            views.setTextViewText(R.id.location, loc)
            views.setTextViewText(R.id.dates, (times?.get(0) ?: "1/1") + "–" + (times?.get(times?.lastIndex ?: 0) ?: "1/5"))
        }
    }

    private fun sendUpdate(context: Context) {
        val intent = Intent(WeatherService.ACTION_UPDATE_WEATHER)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun startService(context: Context) {
        ContextCompat.startForegroundService(context, Intent(context, WeatherService::class.java))
    }

    private fun stopService(context: Context) {
        context.stopService(Intent(context, WeatherService::class.java))
    }
}

