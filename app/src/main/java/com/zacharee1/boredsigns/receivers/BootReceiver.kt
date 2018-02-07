package com.zacharee1.boredsigns.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.*

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Utils.sendWidgetUpdate(context, ImageWidget::class.java, null)
            Utils.sendWidgetUpdate(context, InfoWidget::class.java, null)
            Utils.sendWidgetUpdate(context, NavBarWidget::class.java, null)
            Utils.sendWidgetUpdate(context, WeatherWidget::class.java, null)
            Utils.sendWidgetUpdate(context, WeatherForecastWidget::class.java, null)
            Utils.sendWidgetUpdate(context, Dev1Widget::class.java, null)
            Utils.sendWidgetUpdate(context, Dev2Widget::class.java, null)
        }
    }
}
