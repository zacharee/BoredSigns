package com.zacharee1.boredsigns.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.zacharee1.boredsigns.services.InfoService
import com.zacharee1.boredsigns.widgets.InfoWidget

class Utils {
    companion object {
        fun sendWidgetUpdate(context: Context, clazz: Class<*>, extras: Bundle?) {
            val man = AppWidgetManager.getInstance(context)
            val ids = man.getAppWidgetIds(
                    ComponentName(context, clazz))
            val updateIntent = Intent()
            updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
            updateIntent.putExtra("appWidgetIds", ids)
            if (extras != null) updateIntent.putExtras(extras)
            updateIntent.component = ComponentName(context, clazz)
            context.sendBroadcast(updateIntent)
        }
    }
}