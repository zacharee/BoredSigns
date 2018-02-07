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
import com.zacharee1.boredsigns.widgets.WeatherWidget
import java.io.BufferedInputStream
import java.io.BufferedReader
import java.io.DataInputStream
import java.io.InputStreamReader
import android.opengl.ETC1.getHeight
import android.opengl.ETC1.getWidth
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable



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

        fun isWidgetInUse(clazz: Class<*>, context: Context): Boolean {
            val man = AppWidgetManager.getInstance(context)
            val ids = man.getAppWidgetIds(ComponentName(context, clazz))

            return ids != null && ids.isNotEmpty()
        }

        fun drawableToBitmap(drawable: Drawable): Bitmap {

            if (drawable is BitmapDrawable) {
                return drawable.bitmap
            }

            val bitmap = Bitmap.createBitmap(drawable.intrinsicWidth, drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)

            return bitmap
        }

        fun trimBitmap(bmp: Bitmap?): Bitmap? {
            return if (bmp != null) {
                var minX = bmp.width
                var minY = bmp.height
                var maxX = -1
                var maxY = -1

                for (y in 0 until bmp.height) {
                    for (x in 0 until bmp.width) {
                        val alpha = (bmp.getPixel(x, y) shr 24) and 255
                        if (alpha > 0) {
                            if (x < minX) minX = x
                            if (x > maxX) maxX = x
                            if (y < minY) minY = y
                            if (y > maxY) maxY = y
                        }
                    }
                }

                if (maxX < minX || maxY < minY) null
                else Bitmap.createBitmap(
                        bmp,
                        minX,
                        minY,
                        maxX - minX + 1,
                        maxY - minY + 1
                )
            } else null
        }
    }
}