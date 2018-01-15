package com.zacharee1.boredsigns.fragments

import android.content.Intent
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.LocationPickerActivity
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.WeatherWidget

class WeatherFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_weather)

        setSwitchStatesAndListeners()
    }

    private fun setSwitchStatesAndListeners() {
        val celsius = findPreference("weather_unit") as SwitchPreference
        val useCurrent = findPreference("use_location") as SwitchPreference
        val pickLocation = findPreference("manual_location")
        pickLocation.isEnabled = !useCurrent.isChecked

        val listener = Preference.OnPreferenceChangeListener {
            pref, any ->
            val extras = Bundle()

            if (pref.key == "weather_unit") {
                extras.putBoolean("weather_unit", !(any as Boolean))
            }

            Utils.sendWidgetUpdate(context, WeatherWidget::class.java, extras)

            if (pref.key == "use_location") {
                pickLocation.isEnabled = !(any as Boolean)
            }
            true
        }

        celsius.onPreferenceChangeListener = listener
        useCurrent.onPreferenceChangeListener = listener
        pickLocation.setOnPreferenceClickListener {
            startActivity(Intent(context, LocationPickerActivity::class.java))
            true
        }
    }
}