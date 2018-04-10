package com.zacharee1.boredsigns.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.preference.PreferenceManager
import android.provider.Settings
import android.widget.RemoteViews
import android.widget.Toast
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.views.NavBarButton

class NavBarWidget : AppWidgetProvider() {
    companion object {
        const val BUTTONS_ORDER = "button_order"
        const val DEFAULT_ORDER = "recents|home|back"
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
        if (enabledServices == null || !enabledServices.contains(context.packageName)) {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
            Toast.makeText(context, context.resources.getText(R.string.enable_accessibility), Toast.LENGTH_LONG).show()
            return
        }

        val prefs = PreferenceManager.getDefaultSharedPreferences(context)

        val buttonsOrder = prefs.getString(BUTTONS_ORDER, DEFAULT_ORDER)
        val buttonsList = buttonsOrder.split("|")

        val views = RemoteViews(context.packageName, R.layout.navbar_widget)

        views.removeAllViews(R.id.nav_bar)

        for (button in buttonsList) {
            val navButton = NavBarButton(context, button)

            val new = RemoteViews(context.packageName, navButton.layoutId)
            new.setImageViewBitmap(R.id.image, Utils.getResizedBitmap(navButton.icon, 100, 100))
            new.setInt(R.id.image, "setColorFilter", prefs.getInt("nav_button_color", Color.WHITE))
            val pendingIntent = PendingIntent.getBroadcast(context, 0, Intent(navButton.info?.action ?: ""), 0)
            new.setOnClickPendingIntent(R.id.button, pendingIntent)

            views.addView(R.id.nav_bar, new)
        }

        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}

