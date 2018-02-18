package com.zacharee1.boredsigns.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.preference.PreferenceManager
import android.widget.ImageView
import android.widget.LinearLayout
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.ImagePickerActivity
import com.zacharee1.boredsigns.services.NavBarAccessibility
import com.zacharee1.boredsigns.util.Utils

@SuppressLint("ViewConstructor")
class NavBarButton(context: Context, var key: String?) : LinearLayout(context) {
    val layoutId = R.layout.navbar_image
    var name: String = {
        val which = when (key) {
            "home" -> R.string.home
            "recents" -> R.string.recents
            "back" -> R.string.back
            "power" -> R.string.power
            "qs" -> R.string.qs
            "split" -> R.string.splitscreen
            "notif" -> R.string.notifications
            else -> 0
        }

        if (which != 0) context.resources.getString(which) else ""
    }.invoke()
    var icon = {
        val prefUri = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
        if (prefUri == null) {
            val which = when (key) {
                "home" -> R.drawable.ic_radio_button_checked_black_24dp
                "recents" -> R.drawable.ic_crop_square_black_24dp
                "back" -> R.drawable.ic_arrow_back_black_24dp
                "power" -> R.drawable.ic_power_settings_new_black_24dp
                "qs" -> R.drawable.toggle_off
                "split" -> R.drawable.split_screen
                "notif" -> R.drawable.ic_notifications_none_black_24dp
                else -> R.drawable.ic_help_outline_black_24dp
            }

            context.resources.getDrawable(which, null)
        } else {
            try {
                Drawable.createFromStream(context.contentResolver.openInputStream(Uri.parse(prefUri)), prefUri)
            } catch (e: Exception) {
                context.resources.getDrawable(R.drawable.ic_help_outline_black_24dp, null)
            }
        }
    }
    var action: String = {
        when (key) {
            "home" -> NavBarAccessibility.HOME
            "recents" -> NavBarAccessibility.RECENTS
            "back" -> NavBarAccessibility.BACK
            "power" -> NavBarAccessibility.POWER
            "qs" -> NavBarAccessibility.QS
            "split" -> NavBarAccessibility.SPLIT
            "notif" -> NavBarAccessibility.NOTIFS
            else -> ""
        }
    }.invoke()

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == this.key) {
            setIcon()
        }
    }

    init {
        layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
        (layoutParams as LinearLayout.LayoutParams).weight = 1F
        PreferenceManager.getDefaultSharedPreferences(context).registerOnSharedPreferenceChangeListener(listener)

        setLayoutIdAndInflate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        PreferenceManager.getDefaultSharedPreferences(context).unregisterOnSharedPreferenceChangeListener(listener)
    }

    private fun setLayoutIdAndInflate() {
        inflate(context, layoutId, this)
        setIcon()

        id = when(key) {
            "home" -> R.id.home
            "recents" -> R.id.recents
            "back" -> R.id.back
            "power" -> R.id.power
            "qs" -> R.id.qs
            "split" -> R.id.split
            "notif" -> R.id.notifs
            else -> 0
        }
    }

    private fun setIcon() {
        findViewById<ImageView>(R.id.image)?.let {
            it.setImageBitmap(Utils.drawableToBitmap(icon.invoke()))
            it.setColorFilter(PreferenceManager.getDefaultSharedPreferences(context).getInt("nav_button_color", Color.WHITE))
        }
    }
}