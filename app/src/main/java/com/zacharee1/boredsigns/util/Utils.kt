package com.zacharee1.boredsigns.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceManager
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability
import com.google.android.gms.common.api.GoogleApi
import com.zacharee1.boredsigns.App
import com.zacharee1.boredsigns.receivers.BootReceiver
import com.zacharee1.boredsigns.services.InfoService
import com.zacharee1.boredsigns.widgets.InfoWidget
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStreamReader

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

        fun hasInternet(): Boolean {
            try {
                val process = Runtime.getRuntime().exec("ping -c 1 8.8.8.8")
                process.waitFor()

                val reader = BufferedReader(InputStreamReader(process.inputStream))

                val result = StringBuilder()

                while (true) {
                    result.append(reader.readLine() ?: break)
                }

                return !(result.contains("unreachable"))
            } catch (e: Exception) {
                return false
            }
        }

        fun playServicesWorking(context: Context): Boolean {
            val avail = GoogleApiAvailability.getInstance()
            val result = avail.isGooglePlayServicesAvailable(context)
            return result == ConnectionResult.SUCCESS
        }
    }
}