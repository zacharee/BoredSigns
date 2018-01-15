package com.zacharee1.boredsigns.activities

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import com.google.android.gms.location.places.ui.PlacePicker
import com.zacharee1.boredsigns.services.WeatherService
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.WeatherWidget

class LocationPickerActivity : AppCompatActivity() {
    companion object {
        val REQUEST_CODE = 102
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        startActivityForResult(PlacePicker.IntentBuilder().build(this), REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent) {
        super.onActivityResult(requestCode, resultCode, data)

        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE) {
                val prefs = PreferenceManager.getDefaultSharedPreferences(this)
                prefs.edit().let {
                    val place = PlacePicker.getPlace(this, data)
                    it.putFloat("weather_lat", place.latLng.latitude.toFloat())
                    it.putFloat("weather_lon", place.latLng.longitude.toFloat())
                }.apply()

                Utils.sendWidgetUpdate(this, WeatherWidget::class.java, null)
            }
        }

        finish()
    }
}
