package com.zacharee1.boredsigns.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.zacharee1.boredsigns.services.InfoService
import com.zacharee1.boredsigns.widgets.InfoWidget

class Utils {
    companion object {
        fun sendWidgetUpdate(context: Context, clazz: Class<*>) {
            val man = AppWidgetManager.getInstance(context)
            val ids = man.getAppWidgetIds(
                    ComponentName(context, clazz))
            val updateIntent = Intent()
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra("appWidgetIds", ids)
            updateIntent.component = ComponentName(context, clazz)
            context.sendBroadcast(updateIntent)
        }
    }
}