package com.zacharee1.boredsigns.fragments

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.location.LocationManager
import android.os.Bundle
import android.preference.Preference
import android.preference.PreferenceFragment
import android.preference.SwitchPreference
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.activities.LocationPickerActivity
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.WeatherForecastWidget
import com.zacharee1.boredsigns.widgets.WeatherWidget

class WeatherFragment : PreferenceFragment() {
    private val broadcastReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == LocationManager.PROVIDERS_CHANGED_ACTION) {
                disableCurrentLocationIfNoLocationAvailable()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        addPreferencesFromResource(R.xml.prefs_weather)

        val filter = IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION)
        context.registerReceiver(broadcastReceiver, filter)

        setSwitchStatesAndListeners()
        disableCurrentLocationIfNoLocationAvailable()
    }

    override fun onDestroy() {
        super.onDestroy()

        context.unregisterReceiver(broadcastReceiver)
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

            if (Utils.isWidgetInUse(WeatherWidget::class.java, context)) Utils.sendWidgetUpdate(context, WeatherWidget::class.java, extras)
            if (Utils.isWidgetInUse(WeatherForecastWidget::class.java, context)) Utils.sendWidgetUpdate(context, WeatherForecastWidget::class.java, extras)

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

    private fun disableCurrentLocationIfNoLocationAvailable() {
        val locMan = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val gps = locMan.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val net = locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

        val current = findPreference("use_location") as SwitchPreference

        if (!gps && !net) {
            current.isChecked = false
            preferenceManager.sharedPreferences.edit().putBoolean("use_location", false).apply()
            current.isEnabled = false
            current.summary = resources.getString(R.string.location_services_needed)
        } else {
            current.isEnabled = true
            current.summary = null
        }
    }
}