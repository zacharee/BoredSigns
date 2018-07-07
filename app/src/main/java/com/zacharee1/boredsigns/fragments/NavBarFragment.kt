package com.zacharee1.boredsigns.fragments

import android.os.Bundle
import android.preference.PreferenceFragment
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.NavBarWidget

class NavBarFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_nav)

        setListeners()
    }

    private fun setListeners() {
        findPreference("nav_button_color").setOnPreferenceChangeListener { _, _ ->
            Utils.sendWidgetUpdate(context, NavBarWidget::class.java, null)
            true
        }
    }
}