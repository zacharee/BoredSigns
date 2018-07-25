package com.zacharee1.boredsigns.util

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Matrix
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.text.Html
import android.util.TypedValue
import android.view.View
import android.widget.TextView
import java.io.BufferedReader
import java.io.InputStreamReader


object Utils {
    fun checkCompatibility(context: Context): Boolean {
        return isPackageInstalled(context, "com.lge.signboard")
                || isPackageInstalled(context, "com.zacharee1.aospsignboard")
    }

    fun isPackageInstalled(context: Context, pkg: String): Boolean {
        return try {
            context.packageManager.getPackageInfo(pkg, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

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

    fun getResizedBitmap(bm: Bitmap?, newWidth: Int, newHeight: Int): Bitmap? {
        return if (bm == null) {
            bm
        } else {
            val width = bm.width
            val height = bm.height
            val scaleWidth = newWidth.toFloat() / width
            val scaleHeight = newHeight.toFloat() / height
            // CREATE A MATRIX FOR THE MANIPULATION
            val matrix = Matrix()
            // RESIZE THE BIT MAP
            matrix.postScale(scaleWidth, scaleHeight)

            // "RECREATE" THE NEW BITMAP
            Bitmap.createBitmap(
                    bm, 0, 0, width, height, matrix, false)
        }
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

    fun parseWeatherIconCode(id: String, code: String): String {
        val isDay = code.contains("d")
        return when (id.toInt()) {
            200 -> "f01e"
            201 -> "f01e"
            202 -> "f01e"
            210 -> "f01e"
            211 -> "f01e"
            212 -> "f01e"
            221 -> "f01e"
            230 -> "f01e"
            231 -> "f01e"
            232 -> "f01e"

            300 -> "f01c"
            301 -> "f01c"
            302 -> "f01c"
            310 -> "f01a"
            311 -> "f01a"
            312 -> "f01a"
            313 -> "f01a"
            314 -> "f01a"
            321 -> "f01a"

            500 -> "f01a"
            501 -> "f019"
            502 -> "f019"
            503 -> "f019"
            504 -> "f019"
            511 -> "f017"
            520 -> "f01a"
            521 -> "f01a"
            522 -> "f01a"
            531 -> "f01a"

            600 -> "f01b"
            601 -> "f01b"
            602 -> "f01b"
            611 -> "f0b5"
            612 -> "f017"
            615 -> "f017"
            616 -> "f017"
            620 -> "f0b5"
            621 -> "f0b5"
            622 -> "f0b5"

            701 -> "f014"
            711 -> "f014"
            721 -> "f014"
            731 -> "f063"
            741 -> "f014"
            751 -> "f082"
            761 -> "f063"
            762 -> "f014"
            771 -> "f021"
            781 -> "f056"

            800 -> if (isDay) "f00d" else "f02e"
            801 -> if (isDay) "f002" else "f086"
            802 -> "f041"
            803 -> "f041"
            804 -> "f013"

            900 -> "f056"
            901 -> "f073"
            902 -> "f073"
            903 -> "f076"
            904 -> "f072"
            905 -> "f021"
            906 -> "f015"

            951 -> if (isDay) "f00d" else "f02e"
            952 -> "f021"
            953 -> "f021"
            954 -> "f021"
            955 -> "f021"
            956 -> "f050"
            957 -> "f050"
            958 -> "f050"
            959 -> "f050"
            960 -> "f01e"
            961 -> "f01e"
            962 -> "f073"
            else -> "f00d"
        }
    }

    fun processBmp(code: String?, context: Context): Bitmap {
        val tv = TextView(context)
        tv.typeface = WeatherFonts.getWeather(context)
        tv.setTextColor(Color.WHITE)
        tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, 110f)
        tv.textAlignment = View.TEXT_ALIGNMENT_CENTER
        tv.setText(Html.fromHtml("&#x$code", 0), TextView.BufferType.SPANNABLE)

        val bmp = Bitmap.createBitmap(150, 150, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bmp)
        tv.layout(0, 0, bmp.width, bmp.height)
        tv.draw(canvas)

        return bmp
    }

    private var cpuLast = arrayListOf("0", "0", "0", "0", "0", "0")
    private var cpuLastSum = 0

    fun parseCpuInfo(): ArrayList<String> {
        val proc = Runtime.getRuntime().exec("head -1 /proc/stat")
        val reader = BufferedReader(InputStreamReader(proc.inputStream))
        proc.waitFor()

        val ret = ArrayList<String>()

        try {
            val line = reader.readLine()
            val split = ArrayList(line.replace("cpu", "").trim().split(" "))

            val user = split[0].toInt()
            val nice = split[1].toInt()
            val system = split[2].toInt()
            val idle = split[3].toInt()
            val iowait = split[4].toInt()
            val irq = split[5].toInt()
            val softirq = split[6].toInt()
            val steal = split[7].toInt()

            val sum = user + nice + system + idle + iowait + irq + softirq + steal
            val delta = sum - cpuLastSum
            val idleDelta = (split[4].toInt() + split[3].toInt()) - (cpuLast[4].toInt() + cpuLast[3].toInt())
            val used = delta - idleDelta
            val percent = 100 * used / if (delta == 0) 1 else delta

            cpuLast = split
            cpuLastSum = sum

            ret.add("$percent% Load")

            for (i in 0 until Runtime.getRuntime().availableProcessors()) {
                val freqProc = Runtime.getRuntime().exec("cat /sys/devices/system/cpu/cpu$i/cpufreq/scaling_cur_freq")
                val freqReader = BufferedReader(InputStreamReader(freqProc.inputStream))
                freqProc.waitFor()

                try {
                    val freq = freqReader.readLine().toInt() / 1000
                    ret.add("$freq MHz")
                } catch (e: Exception) {}
            }
        } catch (e: IllegalStateException) {
            e.printStackTrace()
        }

        return ret
    }
}