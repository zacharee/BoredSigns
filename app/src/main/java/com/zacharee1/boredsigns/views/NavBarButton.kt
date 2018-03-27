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
    companion object {
        const val HOME = "home"
        const val RECENTS = "recents"
        const val BACK = "back"
        const val POWER = "power"
        const val QS = "qs"
        const val SPLIT = "split"
        const val NOTIF = "notif"
        const val ASSIST = "assist"
    }

    val layoutId = R.layout.navbar_image
    var name: String = {
        val which = when (key) {
            HOME -> R.string.home
            RECENTS -> R.string.recents
            BACK -> R.string.back
            POWER -> R.string.power
            QS -> R.string.qs
            SPLIT -> R.string.splitscreen
            NOTIF -> R.string.notifications
            ASSIST -> R.string.assist
            else -> 0
        }

        if (which != 0) context.resources.getString(which) else ""
    }.invoke()
    val icon = {
        val prefUri = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
        if (prefUri == null) {
            val which = when (key) {
                HOME -> R.drawable.ic_radio_button_checked_black_24dp
                RECENTS -> R.drawable.ic_crop_square_black_24dp
                BACK -> R.drawable.ic_arrow_back_black_24dp
                POWER -> R.drawable.ic_power_settings_new_black_24dp
                QS -> R.drawable.toggle_off
                SPLIT -> R.drawable.split_screen
                NOTIF -> R.drawable.ic_notifications_none_black_24dp
                ASSIST -> R.drawable.ic_assistant_black_24dp
                else -> R.drawable.ic_help_outline_black_24dp
            }

            context.resources.getDrawable(which, null)!!
        } else {
            try {
                Drawable.createFromStream(context.contentResolver.openInputStream(Uri.parse(prefUri)), prefUri) ?: throw Exception()
            } catch (e: Exception) {
                context.resources.getDrawable(R.drawable.ic_help_outline_black_24dp, null)!!
            }
        }
    }
    val action: String = {
        when (key) {
            HOME -> NavBarAccessibility.HOME
            RECENTS -> NavBarAccessibility.RECENTS
            BACK -> NavBarAccessibility.BACK
            POWER -> NavBarAccessibility.POWER
            QS -> NavBarAccessibility.QS
            SPLIT -> NavBarAccessibility.SPLIT
            NOTIF -> NavBarAccessibility.NOTIFS
            ASSIST -> NavBarAccessibility.ASSIST
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
            HOME -> R.id.home
            RECENTS -> R.id.recents
            BACK -> R.id.back
            POWER -> R.id.power
            QS -> R.id.qs
            SPLIT -> R.id.split
            NOTIF -> R.id.notifs
            ASSIST -> R.id.assist
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