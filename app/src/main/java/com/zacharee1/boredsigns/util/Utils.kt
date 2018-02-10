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
import com.zacharee1.boredsigns.R


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

        fun parseWeatherIconCode(id: String, code: String): Int {
            val isDay = code.contains("d")
            return when (id.toInt()) {
                200 -> R.drawable.storm_5
                201 -> R.drawable.storm_5
                202 -> R.drawable.storm_5
                210 -> R.drawable.storm_1
                211 -> R.drawable.storm_1
                212 -> R.drawable.storm_21
                221 -> if (isDay) R.drawable.storm else R.drawable.storm_12
                230 -> R.drawable.storm_5
                231 -> R.drawable.storm_5
                232 -> R.drawable.storm_5

                300 -> R.drawable.sprinkle_2
                301 -> R.drawable.sprinkle_2
                302 -> R.drawable.sprinkle_2
                310 -> R.drawable.sprinkle_2
                311 -> R.drawable.sprinkle_2
                312 -> R.drawable.sprinkle_2
                313 -> R.drawable.sprinkle_2
                314 -> if (isDay) R.drawable.sprinkle else R.drawable.sprinkle_1
                321 -> if (isDay) R.drawable.sprinkle else R.drawable.sprinkle_1

                500 -> R.drawable.sprinkle_2
                501 -> R.drawable.rain_3
                502 -> R.drawable.rain_3
                503 -> R.drawable.rain_3
                504 -> R.drawable.rain_3
                511 -> R.drawable.sleet_2
                520 -> if (isDay) R.drawable.sprinkle else R.drawable.sprinkle_1
                521 -> if (isDay) R.drawable.sprinkle else R.drawable.sprinkle_1
                522 -> if (isDay) R.drawable.sprinkle else R.drawable.sprinkle_1
                531 -> if (isDay) R.drawable.sprinkle else R.drawable.sprinkle_1

                600 -> R.drawable.snowy
                601 -> R.drawable.snowy
                602 -> R.drawable.snowy
                611 -> R.drawable.sleet_2
                612 -> if (isDay) R.drawable.sleet else R.drawable.sleet_1
                615 -> R.drawable.sleet_2
                616 -> R.drawable.sleet_2
                620 -> if (isDay) R.drawable.snowy_1 else R.drawable.snowy_2
                621 -> if (isDay) R.drawable.snowy_1 else R.drawable.snowy_2
                622 -> if (isDay) R.drawable.snowy_1 else R.drawable.snowy_2

                701 -> R.drawable.haze
                711 -> R.drawable.haze
                721 -> R.drawable.haze
                731 -> R.drawable.storm_2
                741 -> R.drawable.haze
                751 -> R.drawable.storm_2
                761 -> R.drawable.storm_2
                762 -> R.drawable.haze
                771 -> R.drawable.wind
                781 -> R.drawable.tornado

                800 -> if (isDay) R.drawable.sun else R.drawable.moon
                801 -> if (isDay) R.drawable.sunny else R.drawable.cloudy_night
                802 -> if (isDay) R.drawable.sunny else R.drawable.cloudy_night
                803 -> if (isDay) R.drawable.cloudy_night_2 else R.drawable.cloudy_night
                804 -> R.drawable.cloud

                900 -> R.drawable.tornado
                901 -> R.drawable.storm_21
                902 -> R.drawable.hurricane_1
                903 -> if (isDay) R.drawable.sun else R.drawable.moon
                904 -> if (isDay) R.drawable.sun else R.drawable.moon
                905 -> R.drawable.wind
                906 -> R.drawable.hail_2

                951 -> if (isDay) R.drawable.sun else R.drawable.moon
                952 -> if (isDay) R.drawable.wind_13 else R.drawable.wind
                953 -> if (isDay) R.drawable.wind_13 else R.drawable.wind
                954 -> if (isDay) R.drawable.wind_13 else R.drawable.wind
                955 -> if (isDay) R.drawable.wind_13 else R.drawable.wind
                956 -> R.drawable.wind
                957 -> R.drawable.wind
                958 -> R.drawable.wind
                959 -> R.drawable.wind
                960 -> R.drawable.storm_21
                961 -> R.drawable.storm_21
                962 -> R.drawable.hurricane_1
                else -> R.drawable.sunny
            }
        }
    }
}