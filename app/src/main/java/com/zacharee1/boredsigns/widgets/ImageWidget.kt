package com.zacharee1.boredsigns.widgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.widget.RemoteViews

import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.util.Utils

class ImageWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (perm in PermissionsActivity.IMAGE_REQUEST) {
            if (context.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(context, PermissionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("class", this::class.java)
                context.startActivity(intent)
                return
            }
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)
        val uriString = prefs.getString("image_picker", null)
        val views = RemoteViews(context.packageName, R.layout.image_widget)

        var bitmap: Bitmap? = null

        if (uriString != null) {
            try {
                val stream = context.contentResolver.openInputStream(Uri.parse(uriString))
                bitmap = Utils.getResizedBitmap(BitmapFactory.decodeStream(stream, null, null), 1040, 160)

                views.setImageViewBitmap(R.id.main, bitmap)
            } catch (e: Exception) {
                prefs.edit().putString("image_picker", null).apply()
                views.setImageViewResource(R.id.main, R.drawable.example)
            }
        } else {
            views.setImageViewResource(R.id.main, R.drawable.example)
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views)
        bitmap?.recycle()
    }
}

