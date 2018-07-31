package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.services.VolumeService

class MediaVolumeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ContextCompat.startForegroundService(context, Intent(context, VolumeService::class.java))

        val views = RemoteViews(context.packageName, R.layout.volume_widget_layout)
        val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = audioMan.getStreamVolume(AudioManager.STREAM_MUSIC)
        val maxVol = audioMan.getStreamMaxVolume(AudioManager.STREAM_MUSIC)

        val icon = when {
            currentVol > (0.5 * maxVol) -> R.drawable.ic_volume_up_black_24dp
            currentVol > 0 -> R.drawable.ic_volume_down_black_24dp
            else -> R.drawable.ic_volume_mute_black_24dp
        }

        views.setImageViewResource(R.id.vol_ico, icon)

        views.setTextViewText(R.id.vol_type, context.resources.getText(R.string.media))
        views.setInt(R.id.vol_stat, "setMax", maxVol)
        views.setInt(R.id.vol_stat, "setProgress", currentVol)

        val upIntent = PendingIntent.getBroadcast(context, 0, Intent(VolumeService.ACTION_MEDIA_UP), 0)
        val downIntent = PendingIntent.getBroadcast(context, 0, Intent(VolumeService.ACTION_MEDIA_DOWN), 0)

        views.setOnClickPendingIntent(R.id.vol_up, upIntent)
        views.setOnClickPendingIntent(R.id.vol_down, downIntent)

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }

    override fun onDisabled(context: Context?) {
        super.onDisabled(context)

        context?.stopService(Intent(context, VolumeService::class.java))
    }

    override fun onDeleted(context: Context?, appWidgetIds: IntArray?) {
        super.onDeleted(context, appWidgetIds)

        context?.stopService(Intent(context, VolumeService::class.java))
    }
}

