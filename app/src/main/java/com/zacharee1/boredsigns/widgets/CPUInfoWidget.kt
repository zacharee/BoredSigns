package com.zacharee1.boredsigns.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.services.CPUInfoService
import com.zacharee1.boredsigns.util.Utils

class CPUInfoWidget : AppWidgetProvider() {
    private var enabled = false

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (enabled) ContextCompat.startForegroundService(context, Intent(context, CPUInfoService::class.java))

        val views = RemoteViews(context.packageName, R.layout.cpu_widget)
        views.removeAllViews(R.id.content)

        val info = Utils.parseCpuInfo()

        if (info.isEmpty()) {
            val tv = RemoteViews(context.packageName, R.layout.cpu_textview)
            tv.setTextViewText(R.id.usage, context.resources.getText(R.string.unable_to_parse_cpu_info))
            views.addView(R.id.content, tv)
        } else {
            for (s in info) {
                val tv = RemoteViews(context.packageName, R.layout.cpu_textview)
                tv.setTextViewText(R.id.usage, s)
                views.addView(R.id.content, tv)
            }
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onEnabled(context: Context?) {
        enabled = true
    }

    override fun onDisabled(context: Context?) {
        enabled = false

        context?.stopService(Intent(context, CPUInfoService::class.java))
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        enabled = false

        context?.stopService(Intent(context, CPUInfoService::class.java))
    }
}

