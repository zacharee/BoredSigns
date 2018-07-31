package com.zacharee1.boredsigns.services

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.support.v4.app.NotificationCompat
import android.view.Choreographer
import android.view.WindowManager
import com.codemonkeylabs.fpslibrary.Calculation
import com.codemonkeylabs.fpslibrary.FPSConfig
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.Dev1Widget


class Dev1Service : Service() {
    companion object {
        var FPS = 60L
        var USED_MEM = 1024L
        var CHARGE_RATE = 0
    }

    private lateinit var choreographer: Choreographer
    private lateinit var callback: FPSFrameCallback

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    override fun onCreate() {
        startForeground()
        choreographer = Choreographer.getInstance()

        val config = object : FPSConfig() {}
        val display = (getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay

        config.deviceRefreshRateInMs = 1000 / display.refreshRate
        config.refreshRate = display.refreshRate

        callback = FPSFrameCallback(config, this)
        startListening()
        super.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopForeground(true)
        choreographer.removeFrameCallback(callback)
    }

    private fun startForeground() {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.N_MR1) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(NotificationChannel("dev1",
                    resources.getString(R.string.dev_widget_1_title), NotificationManager.IMPORTANCE_LOW))
        }

        startForeground(1337,
                NotificationCompat.Builder(this, "dev1")
                        .setSmallIcon(R.mipmap.ic_launcher_boredsigns)
                        .setPriority(NotificationCompat.PRIORITY_MIN)
                        .build())
    }

    private fun startListening() {
        choreographer.postFrameCallback(callback)
    }


    /**
     * Created by brianplummer on 8/29/15.
     */
    class FPSFrameCallback(private var fpsConfig: FPSConfig?, private var context: Context) : Choreographer.FrameCallback {
        private val dataSet: MutableList<Long> //holds the frame times of the sample set
        private var enabled = true
        private var startSampleTimeInNs: Long = 0

        init {
            dataSet = ArrayList()
        }

        fun setEnabled(enabled: Boolean) {
            this.enabled = enabled
        }

        override fun doFrame(frameTimeNanos: Long) {
            //if not enabled then we bail out now and don't register the callback
            if (!enabled) {
                destroy()
                return
            }

            //initial case
            if (startSampleTimeInNs == 0L) {
                startSampleTimeInNs = frameTimeNanos
            } else if (fpsConfig!!.frameDataCallback != null) {
                val start = dataSet[dataSet.size - 1]
                val droppedCount = Calculation.droppedCount(start, frameTimeNanos, fpsConfig!!.deviceRefreshRateInMs)
                fpsConfig!!.frameDataCallback.doFrame(start, frameTimeNanos, droppedCount)
            }// only invoked for callbacks....

            //we have exceeded the sample length ~700ms worth of data...we should push results and save current
            //frame time in new list
            if (isFinishedWithSample(frameTimeNanos)) {
                collectSampleAndSend(frameTimeNanos)
            }

            // add current frame time to our list
            dataSet.add(frameTimeNanos)

            //we need to register for the next frame callback
            Choreographer.getInstance().postFrameCallback(this)
        }

        private fun collectSampleAndSend(frameTimeNanos: Long) {
            //this occurs only when we have gathered over the sample time ~700ms
            val dataSetCopy = ArrayList<Long>()
            dataSetCopy.addAll(dataSet)

            val droppedSet = Calculation.getDroppedSet(fpsConfig, dataSetCopy)
            val answer = Calculation.calculateMetric(fpsConfig, dataSetCopy, droppedSet)

            val actMan = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()

            actMan.getMemoryInfo(memInfo)

            val batMan = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager

            FPS = answer.value
            USED_MEM = memInfo.totalMem / 0x100000L - memInfo.availMem / 0x100000L
            CHARGE_RATE = -batMan.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW) / 1000

            Utils.sendWidgetUpdate(context, Dev1Widget::class.java, null)

            // clear data
            dataSet.clear()

            //reset sample timer to last frame
            startSampleTimeInNs = frameTimeNanos
        }

        /**
         * returns true when sample length is exceed
         * @param frameTimeNanos current frame time in NS
         * @return
         */
        private fun isFinishedWithSample(frameTimeNanos: Long): Boolean {
            return frameTimeNanos - startSampleTimeInNs > fpsConfig!!.sampleTimeInNs
        }

        private fun destroy() {
            dataSet.clear()
            fpsConfig = null
        }

    }
}
