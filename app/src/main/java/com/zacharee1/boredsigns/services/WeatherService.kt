package com.zacharee1.boredsigns.services

import android.Manifest
import android.app.IntentService
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.WeatherWidget
import zh.wang.android.yweathergetter4a.WeatherInfo
import zh.wang.android.yweathergetter4a.YahooWeather
import zh.wang.android.yweathergetter4a.YahooWeatherInfoListener
import java.util.*

class WeatherService : Service(), YahooWeatherInfoListener {
    companion object {
        val ACTION_UPDATE_WEATHER = "com.zacharee1.boredsigns.action.UPDATE_WEATHER"

        val EXTRA_TEMP = "temp"
        val EXTRA_LOC = "loc"
        val EXTRA_DESC = "desc"
        val EXTRA_ICON = "icon"

        val WHICH_UNIT = "weather_unit"
    }

    private var useCelsius: Boolean = true
    private lateinit var prefs: SharedPreferences

    private lateinit var locClient: FusedLocationProviderClient

    private val locReq: LocationRequest = LocationRequest().setSmallestDisplacement(300F)
    private val locCallback: LocationCallback = object : LocationCallback() {
        override fun onLocationResult(p0: LocationResult) {
            for (location in p0.locations) {
                onHandleIntent(ACTION_UPDATE_WEATHER)
            }
        }
    }

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            onHandleIntent(p1?.action)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onHandleIntent(intent?.action)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        locClient = LocationServices.getFusedLocationProviderClient(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(ACTION_UPDATE_WEATHER))
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    override fun gotWeatherInfo(weatherInfo: WeatherInfo?, errorType: YahooWeather.ErrorType?) {
        if (errorType == null) {
            val extras = Bundle()
            extras.putString(EXTRA_TEMP, weatherInfo?.currentTemp.toString() + "°" + if (useCelsius) "C" else "F")
            extras.putString(EXTRA_LOC, weatherInfo?.locationCity + ", " + weatherInfo?.locationRegion)
            extras.putString(EXTRA_DESC, weatherInfo?.currentText)
            extras.putParcelable(EXTRA_ICON, weatherInfo?.currentConditionIcon)

            Utils.sendWidgetUpdate(this, WeatherWidget::class.java, extras)
        } else {
            Toast.makeText(this, String.format(Locale.US, resources.getString(R.string.error_retrieving_weather), errorType.toString()), Toast.LENGTH_SHORT).show()

        }
    }

    private fun onHandleIntent(action: String?) {
        when (action) {
            ACTION_UPDATE_WEATHER -> {
                useCelsius = prefs.getBoolean(WHICH_UNIT, true)
                val weather = YahooWeather.getInstance()
                weather.unit = if (useCelsius) YahooWeather.UNIT.CELSIUS else YahooWeather.UNIT.FAHRENHEIT
                weather.setNeedDownloadIcons(true)

                if (prefs.getBoolean("use_location", true)) {
                    weather.queryYahooWeatherByGPS(this, this)
                } else {
                    val lat = prefs.getFloat("weather_lat", 51.508530F).toDouble()
                    val lon = prefs.getFloat("weather_lon", -0.076132F).toDouble()

                    weather.queryYahooWeatherByLatLon(this, lat, lon, this)
                }
            }
        }
    }

    private fun startLocationUpdates() {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locClient.requestLocationUpdates(locReq, locCallback, null)
        }
    }

    private fun stopLocationUpdates() {
        locClient.removeLocationUpdates(locCallback)
    }
}
