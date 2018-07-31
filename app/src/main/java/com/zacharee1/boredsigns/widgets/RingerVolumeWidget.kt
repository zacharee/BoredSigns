package com.zacharee1.boredsigns.widgets

import android.app.NotificationManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.provider.Settings
import android.support.v4.content.ContextCompat
import android.widget.RemoteViews
import android.widget.Toast
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.services.VolumeService

class RingerVolumeWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        ContextCompat.startForegroundService(context, Intent(context, VolumeService::class.java))

        val notifMan = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (!notifMan.isNotificationPolicyAccessGranted) {
            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS))
            Toast.makeText(context, context.resources.getText(R.string.grant_notification_access), Toast.LENGTH_SHORT).show()
            return
        }

        val views = RemoteViews(context.packageName, R.layout.volume_widget_layout)
        val audioMan = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val currentVol = audioMan.getStreamVolume(AudioManager.STREAM_RING)
        val maxVol = audioMan.getStreamMaxVolume(AudioManager.STREAM_RING)

        val icon = when (audioMan.ringerMode) {
            AudioManager.RINGER_MODE_VIBRATE -> R.drawable.ic_vibration_black_24dp
            AudioManager.RINGER_MODE_SILENT -> R.drawable.ic_volume_mute_black_24dp
            else -> R.drawable.ic_ring_volume_black_24dp
        }
        views.setImageViewResource(R.id.vol_ico, icon)
        views.setTextViewText(R.id.vol_type, context.resources.getText(R.string.ringer))
        views.setInt(R.id.vol_stat, "setMax", maxVol)
        views.setInt(R.id.vol_stat, "setProgress", currentVol)

        val upIntent = PendingIntent.getBroadcast(context, 0, Intent(VolumeService.ACTION_RINGER_UP), 0)
        val downIntent = PendingIntent.getBroadcast(context, 0, Intent(VolumeService.ACTION_RINGER_DOWN), 0)

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

