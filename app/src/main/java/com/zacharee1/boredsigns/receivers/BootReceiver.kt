package com.zacharee1.boredsigns.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.ImageWidget
import com.zacharee1.boredsigns.widgets.InfoWidget
import com.zacharee1.boredsigns.widgets.NavBarWidget
import com.zacharee1.boredsigns.widgets.WeatherWidget

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Utils.sendWidgetUpdate(context, ImageWidget::class.java, null)
            Utils.sendWidgetUpdate(context, InfoWidget::class.java, null)
            Utils.sendWidgetUpdate(context, NavBarWidget::class.java, null)
            Utils.sendWidgetUpdate(context, WeatherWidget::class.java, null)
        }
    }
}
