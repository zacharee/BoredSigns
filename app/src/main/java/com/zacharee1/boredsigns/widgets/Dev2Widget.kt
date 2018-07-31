package com.zacharee1.boredsigns.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.services.Dev2Service

class Dev2Widget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ContextCompat.startForegroundService(context, Intent(context, Dev2Service::class.java))

        val views = RemoteViews(context.packageName, R.layout.dev2_widget)

        views.setTextViewText(R.id.cpu, Dev2Service.CPU.toString())
        views.setTextViewText(R.id.gpu, Dev2Service.GPU.toString())
        views.setTextViewText(R.id.batt, Dev2Service.BATT.toString())

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)

        context?.stopService(Intent(context, Dev2Service::class.java))
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)

        context?.stopService(Intent(context, Dev2Service::class.java))
    }
}

