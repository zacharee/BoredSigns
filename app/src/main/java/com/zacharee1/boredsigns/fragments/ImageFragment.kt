package com.zacharee1.boredsigns.fragments

import android.os.Bundle
import android.preference.PreferenceFragment
import com.zacharee1.boredsigns.R

class ImageFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_image)
    }
}