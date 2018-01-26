package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast

import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.PermissionsActivity
import com.zacharee1.boredsigns.services.NavBarAccessibility
import com.zacharee1.boredsigns.util.Utils

class NavBarWidget : AppWidgetProvider() {
    companion object {
        const val BUTTONS_ORDER = "button_order"
        const val DEFAULT_ORDER = "recents|home|back"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        if (Utils.isAuthed(context) && Utils.isBooted(context)) {
            for (perm in PermissionsActivity.REQUEST) {
                val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
                if (enabledServices == null || !enabledServices.contains(context.packageName)) {
                    val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    context.startActivity(intent)
                    Toast.makeText(context, context.resources.getText(R.string.enable_accessibility), Toast.LENGTH_LONG).show()
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

                val new = RemoteViews(context.packageName, layout)
                new.setInt(R.id.image, "setColorFilter", prefs.getInt("nav_button_color", Color.WHITE))

                views.addView(R.id.nav_bar, new )
                val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(action), 0)
                views.setOnClickPendingIntent(id, pendingIntent)
            }

            appWidgetManager.updateAppWidget(appWidgetIds, views)
        }
    }
}

