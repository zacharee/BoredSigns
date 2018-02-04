package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.support.v4.content.LocalBroadcastManager
import android.view.View
import android.widget.RemoteViews

import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.services.WeatherService

class WeatherForecastWidget : AppWidgetProvider() {
    private var temp: ArrayList<String> = ArrayList()
    private var loc: String? = null
    private var desc: ArrayList<String> = ArrayList()
    private var icon: ArrayList<Bitmap>? = ArrayList()

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
            val t = it.getStringArrayListExtra(WeatherService.EXTRA_TEMP)
            val l = it.getStringExtra(WeatherService.EXTRA_LOC)
            val d = it.getStringArrayListExtra(WeatherService.EXTRA_DESC)
            val i = it.getParcelableArrayListExtra<Bitmap>(WeatherService.EXTRA_ICON)
            if (t != null && l != null && d != null) {
                temp = t
                loc = l
                desc = d
                icon = i

            }
        }

        super.onReceive(context, intent)
    }

    override fun onEnabled(context: Context?) {
        super.onEnabled(context)
        startService(context)
    }

    override fun onRestored(context: Context?, oldWidgetIds: IntArray?, newWidgetIds: IntArray?) {
        super.onRestored(context, oldWidgetIds, newWidgetIds)
        startService(context)
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)
        stopService(context)
    }

    override fun onDisabled(context: Context?) {
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
        if (desc.isEmpty() || loc == null || temp.isEmpty()) {
            sendUpdate(context)
        }
        else {
            views.setViewVisibility(R.id.loading, View.GONE)
            views.setViewVisibility(R.id.refresh, View.VISIBLE)

            val defBmp = BitmapFactory.decodeResource(context.resources, R.drawable.ic_wb_sunny_black_24dp)

            views.setImageViewBitmap(R.id.icon_1, icon?.get(0) ?: defBmp)
            views.setImageViewBitmap(R.id.icon_2, icon?.get(1) ?: defBmp)
            views.setImageViewBitmap(R.id.icon_3, icon?.get(2) ?: defBmp)
            views.setImageViewBitmap(R.id.icon_4, icon?.get(3) ?: defBmp)
            views.setImageViewBitmap(R.id.icon_5, icon?.get(4) ?: defBmp)

            views.setTextViewText(R.id.location, loc)

            views.setTextViewText(R.id.temp_1, temp[0])
            views.setTextViewText(R.id.temp_2, temp[1])
            views.setTextViewText(R.id.temp_3, temp[2])
            views.setTextViewText(R.id.temp_4, temp[3])
            views.setTextViewText(R.id.temp_5, temp[4])
        }
    }

    private fun sendUpdate(context: Context) {
        val intent = Intent(WeatherService.ACTION_UPDATE_WEATHER)
        LocalBroadcastManager.getInstance(context).sendBroadcast(intent)
    }

    private fun startService(context: Context?) {
        context?.startService(Intent(context, WeatherService::class.java))
    }

    private fun stopService(context: Context?) {
        context?.stopService(Intent(context, WeatherService::class.java))
    }
}

