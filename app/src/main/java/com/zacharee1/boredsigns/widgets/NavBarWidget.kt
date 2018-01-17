package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.widget.RemoteViews

import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.services.NavBarAccessibility

class NavBarWidget : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        for (perm in PermissionsActivity.REQUEST) {
            if (context.checkCallingOrSelfPermission(perm) != PackageManager.PERMISSION_GRANTED) {
                val intent = Intent(context, PermissionsActivity::class.java)
                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                intent.putExtra("class", this::class.java)
                context.startActivity(intent)
                return
            }
        }

        val views = RemoteViews(context.packageName, R.layout.navbar_widget)

        val homeIntent = Intent(NavBarAccessibility.HOME)
        val recentsIntent = Intent(NavBarAccessibility.RECENTS)
        val backIntent = Intent(NavBarAccessibility.BACK)

        val home = PendingIntent.getBroadcast(context, 0, homeIntent, 0)
        val recents = PendingIntent.getBroadcast(context, 0, recentsIntent, 0)
        val back = PendingIntent.getBroadcast(context, 0, backIntent, 0)

        views.removeAllViews(R.id.nav_bar)
        views.addView(R.id.nav_bar, RemoteViews(context.packageName, R.layout.navbar_recents))
        views.addView(R.id.nav_bar, RemoteViews(context.packageName, R.layout.navbar_home))
        views.addView(R.id.nav_bar, RemoteViews(context.packageName, R.layout.navbar_back))

        views.setOnClickPendingIntent(R.id.home, home)
        views.setOnClickPendingIntent(R.id.recents, recents)
        views.setOnClickPendingIntent(R.id.back, back)

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}

