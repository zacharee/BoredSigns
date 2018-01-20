package com.zacharee1.boredsigns.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.os.Bundle
import android.os.IBinder
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.widget.Toast
import com.google.android.gms.location.*
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.proxies.WeatherProxy
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.WeatherWidget
import github.vatsal.easyweather.Helper.TempUnitConverter
import github.vatsal.easyweather.Helper.WeatherCallback
import github.vatsal.easyweather.WeatherMap
import github.vatsal.easyweather.retrofit.models.WeatherResponseModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.text.WordUtils
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*

class WeatherService : Service() {
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

    private var apiKey: String? = null

    private val prefsListener = SharedPreferences.OnSharedPreferenceChangeListener {
        prefs, s ->
        if (s == "weather_api_key") {
            apiKey = prefs.getString("weather_api_key", null)
            onHandleIntent(ACTION_UPDATE_WEATHER)
        }
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        onHandleIntent(intent?.action)
        apiKey = prefs.getString("weather_api_key", null)
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onCreate() {
        super.onCreate()
        prefs = PreferenceManager.getDefaultSharedPreferences(this)
        prefs.registerOnSharedPreferenceChangeListener(prefsListener)
        locClient = LocationServices.getFusedLocationProviderClient(this)
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(ACTION_UPDATE_WEATHER))
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
    }

    private fun onHandleIntent(action: String?) {
        when (action) {
            ACTION_UPDATE_WEATHER -> {
                useCelsius = prefs.getBoolean(WHICH_UNIT, true)

                if (apiKey == null) {
                    val intent = Intent(this, WeatherProxy::class.java)
                    startActivity(intent)

                    Toast.makeText(this, resources.getText(R.string.add_owm_key), Toast.LENGTH_SHORT).show()
                } else {
                    if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                        locClient.lastLocation.addOnCompleteListener {
                            var lat = it.result.latitude
                            var lon = it.result.longitude

                            val geo = Geocoder(baseContext, Locale.getDefault())
                            val addrs = geo.getFromLocation(lat, lon, 1)

                            if (!prefs.getBoolean("use_location", true)) {
                                lat = prefs.getFloat("weather_lat", 51.508530F).toDouble()
                                lon = prefs.getFloat("weather_lon", -0.076132F).toDouble()
                            }

                            val weather = WeatherMap(this, apiKey)

                            weather.getLocationWeather(lat.toString(), lon.toString(), object : WeatherCallback() {
                                @SuppressLint("CheckResult")
                                override fun success(response: WeatherResponseModel?) {
                                    val extras = Bundle()

                                    val temp = response?.main?.temp
                                    val tempDouble: Double = if (useCelsius) TempUnitConverter.convertToCelsius(temp) else TempUnitConverter.convertToFahrenheit(temp)

                                    val formatted = DecimalFormat("#").format(tempDouble).toString()

                                    extras.putString(EXTRA_TEMP, formatted + "°" + if (useCelsius) "C" else "F")
                                    extras.putString(EXTRA_LOC, addrs[0].locality + ", " + addrs[0].adminArea)
                                    extras.putString(EXTRA_DESC, WordUtils.capitalize(response?.weather?.get(0)?.description))

                                    Utils.sendWidgetUpdate(this@WeatherService, WeatherWidget::class.java, extras)

                                    Observable.fromCallable({asyncLoadUrl(URL(response?.weather?.get(0)?.iconLink))})
                                            .subscribeOn(Schedulers.io())
                                            .observeOn(AndroidSchedulers.mainThread())
                                            .subscribe {
                                                bmp ->
                                                extras.putParcelable(EXTRA_ICON, bmp)
                                                Utils.sendWidgetUpdate(this@WeatherService, WeatherWidget::class.java, extras)
                                            }
                                }

                                override fun failure(error: String?) {
                                    Toast.makeText(this@WeatherService, String.format(Locale.US, resources.getString(R.string.error_retrieving_weather), error), Toast.LENGTH_SHORT).show()
                                }
                            })
                        }
                    }
                }
            }
        }
    }

    private fun asyncLoadUrl(url: URL): Bitmap? {
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            BitmapFactory.decodeStream(connection.inputStream)
        } catch (e: Exception) {
            null
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
