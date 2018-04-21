package com.zacharee1.boredsigns.views

import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.preference.PreferenceManager
import android.util.Base64
import android.widget.ImageView
import android.widget.LinearLayout
import com.zacharee1.boredsigns.R
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

    val layoutId = R.layout.navbar_image

    val name: String
        get() {
            val which = info?.name ?: 0
            return if (which != 0) context.resources.getString(which) else ""
        }

    val icon: Bitmap
        get() {
            val encodedBmp = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
            return try {
                val decodedByteArray = Base64.decode(encodedBmp, Base64.DEFAULT)
                BitmapFactory.decodeByteArray(decodedByteArray, 0, decodedByteArray.size) ?: throw NullPointerException()
            } catch (e: NullPointerException) {
                Utils.drawableToBitmap(context.resources.getDrawable(info?.icon ?: R.drawable.ic_help_outline_black_24dp, null))
            } catch (e: IllegalArgumentException) {
                Utils.drawableToBitmap(context.resources.getDrawable(info?.icon ?: R.drawable.ic_help_outline_black_24dp, null))
            }
        }

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

        inflate(context, layoutId, this)
        setIcon()
    }

    private fun setIcon() {
        findViewById<ImageView>(R.id.image)?.let {
            it.setImageBitmap(icon)
            it.setColorFilter(PreferenceManager.getDefaultSharedPreferences(context).getInt("nav_button_color", Color.WHITE))
        }
    }

    class Info(val name: Int, val id: Int, val icon: Int, val key: String, val action: String)
}