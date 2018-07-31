package com.zacharee1.boredsigns.widgets

import android.app.ActivityManager
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.services.Dev1Service

class Dev1Widget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ContextCompat.startForegroundService(context, Intent(context, Dev1Service::class.java))

        val views = RemoteViews(context.packageName, R.layout.dev1_widget)
        views.setTextViewText(R.id.fps, Dev1Service.FPS.toString())

        val actMan = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()

        actMan.getMemoryInfo(memInfo)

        views.setTextViewText(R.id.mem_info, Dev1Service.USED_MEM.toString() + " / " + memInfo.totalMem / 0x100000L)

        views.setTextViewText(R.id.bat_info, Dev1Service.CHARGE_RATE.toString())

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)

        context?.stopService(Intent(context, Dev1Service::class.java))
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)

        context?.stopService(Intent(context, Dev1Service::class.java))
    }
}

