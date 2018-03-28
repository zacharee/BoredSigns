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

        val INFOS = object : HashMap<String, Info>() {
            init {
                put(HOME, Info(R.string.home, R.id.home, R.drawable.ic_radio_button_checked_black_24dp, HOME, NavBarAccessibility.HOME))
                put(RECENTS, Info(R.string.recents, R.id.recents, R.drawable.ic_crop_square_black_24dp, RECENTS, NavBarAccessibility.RECENTS))
                put(BACK, Info(R.string.back, R.id.back, R.drawable.ic_arrow_back_black_24dp, BACK, NavBarAccessibility.BACK))
                put(POWER, Info(R.string.power, R.id.power, R.drawable.ic_power_settings_new_black_24dp, POWER, NavBarAccessibility.POWER))
                put(QS, Info(R.string.qs, R.id.qs, R.drawable.toggle_off, QS, NavBarAccessibility.QS))
                put(SPLIT, Info(R.string.splitscreen, R.id.split, R.drawable.split_screen, SPLIT, NavBarAccessibility.SPLIT))
                put(NOTIF, Info(R.string.notifications, R.id.notifs, R.drawable.ic_notifications_none_black_24dp, NOTIF, NavBarAccessibility.NOTIFS))
                put(ASSIST, Info(R.string.assist, R.id.assist, R.drawable.ic_assistant_black_24dp, ASSIST, NavBarAccessibility.ASSIST))
            }
        }
    }

    val info = INFOS[key]

    var name: String = {
        val which = info?.name
        if (which != null && which != 0) context.resources.getString(which) else ""
    }.invoke()

    val icon = {
        val prefUri = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
        if (prefUri == null) {
            val which = info?.icon ?: R.drawable.ic_help_outline_black_24dp

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
        info?.action ?: ""
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
        id = info?.id ?: 0

        inflate(context, R.layout.navbar_image, this)
        setIcon()
    }

    private fun setIcon() {
        findViewById<ImageView>(R.id.image)?.let {
            it.setImageBitmap(Utils.drawableToBitmap(icon.invoke()))
            it.setColorFilter(PreferenceManager.getDefaultSharedPreferences(context).getInt("nav_button_color", Color.WHITE))
        }
    }

    class Info(val name: Int, val id: Int, val icon: Int, val key: String, val action: String)
}