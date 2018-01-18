package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.preference.PreferenceManager
import android.widget.RemoteViews

import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.services.NavBarAccessibility

class NavBarWidget : AppWidgetProvider() {
    companion object {
        val BUTTONS_ORDER = "button_order"
        val DEFAULT_ORDER = "recents|home|back"
    }

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

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val buttonsOrder = prefs.getString(BUTTONS_ORDER, DEFAULT_ORDER)
        val buttonsList = buttonsOrder.split("|")

        val views = RemoteViews(context.packageName, R.layout.navbar_widget)

        views.removeAllViews(R.id.nav_bar)

        for (button in buttonsList) {
            var action = ""
            var layout = 0
            var id = 0

            when (button) {
                "home" -> {
                    action = NavBarAccessibility.HOME
                    layout = R.layout.navbar_home
                    id = R.id.home
                }

                "recents" -> {
                    action = NavBarAccessibility.RECENTS
                    layout = R.layout.navbar_recents
                    id = R.id.recents
                }

                "back" -> {
                    action = NavBarAccessibility.BACK
                    layout = R.layout.navbar_back
                    id = R.id.back
                }

                "split" -> {
                    action = NavBarAccessibility.SPLIT
                    layout = R.layout.navbar_splitscreen
                    id = R.id.split
                }

                "qs" -> {
                    action = NavBarAccessibility.QS
                    layout = R.layout.navbar_qs
                    id = R.id.qs
                }

                "notif" -> {
                    action = NavBarAccessibility.NOTIFS
                    layout = R.layout.navbar_notifs
                    id = R.id.notifs
                }

                "power" -> {
                    action = NavBarAccessibility.POWER
                    layout = R.layout.navbar_power
                    id = R.id.power
                }
            }

            views.addView(R.id.nav_bar, RemoteViews(context.packageName, layout))
            val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(action), 0)
            views.setOnClickPendingIntent(id, pendingIntent)
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}

