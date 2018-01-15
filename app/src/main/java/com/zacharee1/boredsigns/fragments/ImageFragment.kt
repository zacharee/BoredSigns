package com.zacharee1.boredsigns.fragments

import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.preference.PreferenceFragment
import android.preference.PreferenceManager
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.prefs.ImagePreference
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.ImageWidget

class ImageFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onResume() {
        super.onResume()
        preferenceScreen?.removeAll()
        addPreferencesFromResource(R.xml.prefs_image)

        setListener()
    }

    private fun setListener() {
        findPreference("image_reset").setOnPreferenceClickListener {
            PreferenceManager.getDefaultSharedPreferences(context).edit().putString("image_picker", null).apply()
            onResume()
            Utils.sendWidgetUpdate(context, ImageWidget::class.java, null)
            true
        }
    }
}