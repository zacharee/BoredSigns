package com.zacharee1.boredsigns.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.content.*
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.os.Bundle
import android.os.IBinder
import android.os.SystemClock
import android.preference.PreferenceManager
import android.support.v4.content.LocalBroadcastManager
import android.util.Log
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.zacharee1.boredsigns.App
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.proxies.WeatherProxy
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.WeatherForecastWidget
import com.zacharee1.boredsigns.widgets.WeatherWidget
import github.vatsal.easyweather.Helper.TempUnitConverter
import github.vatsal.easyweather.Helper.WeatherCallback
import github.vatsal.easyweather.WeatherMap
import github.vatsal.easyweather.retrofit.models.*
import github.vatsal.easyweather.retrofit.models.List
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileNotFoundException
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.nio.charset.Charset
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

class WeatherService : Service() {
    companion object {
        const val ACTION_UPDATE_WEATHER = "com.zacharee1.boredsigns.action.UPDATE_WEATHER"
        const val API_KEY = App.API_KEY //IMPORTANT: Use your own OWM API key here when building for yourself!

        const val EXTRA_TEMP = "temp"
        const val EXTRA_TEMP_EX = "temp_ex"
        const val EXTRA_LOC = "loc"
        const val EXTRA_DESC = "desc"
        const val EXTRA_ICON = "icon"
        const val EXTRA_TIME = "time"

        const val WHICH_UNIT = "weather_unit"
    }

    private var useCelsius: Boolean = true
    private lateinit var prefs: SharedPreferences

    private lateinit var locClient: FusedLocationProviderClient
    private lateinit var alarmManager: AlarmManager

    private lateinit var alarmIntent: PendingIntent

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
        alarmManager = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmIntent = PendingIntent.getService(this, 0, Intent(this, this::class.java), 0)

        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + 7200 * 1000,
                7200 * 1000,
                alarmIntent)

        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            locMan.requestSingleUpdate(LocationManager.GPS_PROVIDER, object : android.location.LocationListener {
                override fun onLocationChanged(p0: Location?) {
                    onHandleIntent(ACTION_UPDATE_WEATHER)
                }

                override fun onProviderDisabled(p0: String?) {

                }

                override fun onProviderEnabled(p0: String?) {

                }

                override fun onStatusChanged(p0: String?, p1: Int, p2: Bundle?) {

                }
            }, null)
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, IntentFilter(ACTION_UPDATE_WEATHER))
        startLocationUpdates()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopLocationUpdates()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        alarmManager.cancel(alarmIntent)
    }

    private fun onHandleIntent(action: String?) {
        when (action) {
            ACTION_UPDATE_WEATHER -> {
                useCelsius = prefs.getBoolean(WHICH_UNIT, true)

                val locMan = getSystemService(Context.LOCATION_SERVICE) as LocationManager
                if ((!locMan.isProviderEnabled(LocationManager.GPS_PROVIDER) && !locMan.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) || !prefs.getBoolean("use_location", true)) {
                    val lat = prefs.getFloat("weather_lat", 51.508530F).toDouble()
                    val lon = prefs.getFloat("weather_lon", -0.076132F).toDouble()
                    getWeather(lat, lon)
                } else if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                    locClient.lastLocation.addOnCompleteListener {
                        it.result?.let {
                            val lat = it.latitude
                            val lon = it.longitude
                            getWeather(lat, lon)
                        }
                    }
                }
            }
        }
    }

    private fun getWeather(lat: Double, lon: Double) {
        try {
            val geo = Geocoder(applicationContext, Locale.getDefault())
            val weather = WeatherMap(applicationContext, API_KEY)
            val addrs = geo.getFromLocation(lat, lon, 1)

            if (isCurrentActivated()) {
                weather.getLocationWeather(lat.toString(), lon.toString(), object : WeatherCallback() {
                    @SuppressLint("CheckResult")
                    override fun success(response: WeatherResponseModel) {
                        val extras = Bundle()

                        val temp = response.main.temp
                        val tempDouble: Double = if (useCelsius) TempUnitConverter.convertToCelsius(temp) else TempUnitConverter.convertToFahrenheit(temp)
                        val time = SimpleDateFormat("h:mm aa", Locale.getDefault()).format(Date(response.dt.toLong() * 1000))

                        val formatted = DecimalFormat("#").format(tempDouble).toString()

                        extras.putString(EXTRA_TEMP, formatted + "°" + if (useCelsius) "C" else "F")
                        extras.putString(EXTRA_LOC, addrs[0].locality + ", " + addrs[0].adminArea)
                        extras.putString(EXTRA_DESC, capitalize(response.weather[0].description))
                        extras.putString(EXTRA_TIME, time)

                        Utils.sendWidgetUpdate(this@WeatherService, WeatherWidget::class.java, extras)

                        Observable.fromCallable({asyncLoadUrl(URL(response.weather[0].iconLink))})
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

            if (isForecastActivated()) {
                ForecastParser().sendRequest(lat.toString(), lon.toString(), object : ForecastCallback {
                    @SuppressLint("CheckResult")
                    override fun onSuccess(model: ForecastResponseModel) {
                        val extras = Bundle()

                        val highTemps = ArrayList<String>()
                        val lowTemps = ArrayList<String>()
                        val times = ArrayList<String>()

                        model.list
                                .map { it.main.temp_max }
                                .map { if (useCelsius) TempUnitConverter.convertToCelsius(it) else TempUnitConverter.convertToFahrenheit(it) }
                                .map { DecimalFormat("#").format(it).toString() }
                                .mapTo(highTemps) { it + "°" + if (useCelsius) "C" else "F" }

                        model.list
                                .map { it.main.temp_min }
                                .map { if (useCelsius) TempUnitConverter.convertToCelsius(it) else TempUnitConverter.convertToFahrenheit(it) }
                                .map { DecimalFormat("#").format(it).toString() }
                                .mapTo(lowTemps) { it + "°" + if (useCelsius) "C" else "F" }

                        model.list.mapTo(times) { SimpleDateFormat("M/d", Locale.getDefault()).format(Date(it.dt.toLong() * 1000)) }

                        extras.putStringArrayList(EXTRA_TEMP, highTemps)
                        extras.putStringArrayList(EXTRA_TEMP_EX, lowTemps)
                        extras.putString(EXTRA_LOC, addrs[0].locality + ", " + addrs[0].adminArea)
                        extras.putStringArrayList(EXTRA_TIME, times)

                        Utils.sendWidgetUpdate(this@WeatherService, WeatherForecastWidget::class.java, extras)

                        val urls = ArrayList<URL>()
                        model.list.mapTo(urls) { URL(it.weather[0].iconLink) }

                        Observable.fromCallable({asyncLoadUrls(urls)})
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    list ->
                                    extras.putParcelableArrayList(EXTRA_ICON, list)
                                    Utils.sendWidgetUpdate(this@WeatherService, WeatherForecastWidget::class.java, extras)
                                }                    }

                    override fun onFail(message: String) {
                        Toast.makeText(this@WeatherService, String.format(Locale.US, resources.getString(R.string.error_retrieving_weather), message), Toast.LENGTH_SHORT).show()
                    }
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val bundle = Bundle()
            bundle.putString("message", e.localizedMessage)
            bundle.putString("stacktrace", Arrays.toString(e.stackTrace))
            FirebaseAnalytics.getInstance(this).logEvent("failed_weather", bundle)
            Toast.makeText(this, String.format(Locale.US, resources.getString(R.string.error_retrieving_weather), e.localizedMessage), Toast.LENGTH_LONG).show()
        }
    }

    private fun capitalize(string: String): String {
        val builder = StringBuilder()
        val words = string.split(" ")

        for (word in words) {
            if (builder.isNotEmpty()) {
                builder.append(" ")
            }

            builder.append(word[0].toUpperCase()).append(word.substring(1, word.length))
        }

        return builder.toString()
    }

    private fun asyncLoadUrl(url: URL): Bitmap {
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            Utils.trimBitmap(BitmapFactory.decodeStream(connection.inputStream)) ?: throw Exception()
        } catch (e: Exception) {
            Utils.drawableToBitmap(resources.getDrawable(R.drawable.ic_wb_sunny_white_24dp, null))
        }
    }

    private fun asyncLoadUrls(urls: ArrayList<URL>): ArrayList<Bitmap> {
        val icons = ArrayList<Bitmap>()

        urls.mapTo(icons) { asyncLoadUrl(it) }

        return icons
    }

    private fun startLocationUpdates() {
        if (checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            locClient.requestLocationUpdates(locReq, locCallback, null)
        }
    }

    private fun stopLocationUpdates() {
        locClient.removeLocationUpdates(locCallback)
    }

    private fun isCurrentActivated(): Boolean {
        return Utils.isWidgetInUse(WeatherWidget::class.java, this)
    }

    private fun isForecastActivated(): Boolean {
        return Utils.isWidgetInUse(WeatherForecastWidget::class.java, this)
    }

    class ForecastParser() {
        private val numToGet = 7
        private val template = "http://api.openweathermap.org/data/2.5/forecast/daily?lat=LAT&lon=LON&cnt=$numToGet&appid=$API_KEY"

        @SuppressLint("CheckResult")
        fun sendRequest(lat: String, lon: String, callback: ForecastCallback) {
            val req = template.replace("LAT", lat).replace("LON", lon)

            try {
                Observable.fromCallable({asyncGetJsonString(URL(req))})
                        .subscribeOn(Schedulers.io())
                        .observeOn(AndroidSchedulers.mainThread())
                        .subscribe {
                            if (it.has("cod") && it.getString("cod") != "200") {
                                callback.onFail(it.getString("message"))
                            } else {
                                try {
                                    callback.onSuccess(parseJsonData(it))
                                } catch (e: Exception) {
                                    callback.onFail(e.localizedMessage)
                                }
                            }
                        }
            } catch (e: Exception) {
                callback.onFail(e.localizedMessage)
            }
        }

        private fun parseJsonData(json: JSONObject): ForecastResponseModel {
            val response = ForecastResponseModel()
            val list = ArrayList<List>()

            val stuff = json.getJSONArray("list")

            for (i in 0 until stuff.length()) {
                val l = List()
                val main = Main()
                val weather = Weather()

                val s = stuff.getJSONObject(i)

                weather.icon = s.getJSONArray("weather").getJSONObject(0).getString("icon")
                main.temp_max = s.getJSONObject("temp").getString("max")
                main.temp_min = s.getJSONObject("temp").getString("min")

                l.weather = arrayOf(weather)
                l.main = main
                l.dt = s.getString("dt")

                list.add(l)
            }

            list.removeAt(0)

            val listArr = arrayOfNulls<List>(list.size)
            response.list = list.toArray(listArr)

            return response
        }

        private fun asyncGetJsonString(url: URL): JSONObject{
            var connection = url.openConnection() as HttpURLConnection

            if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                if (connection.responseCode == HttpURLConnection.HTTP_MOVED_TEMP
                        || connection.responseCode == HttpURLConnection.HTTP_MOVED_PERM
                        || connection.responseCode == HttpURLConnection.HTTP_SEE_OTHER) {
                    val newUrl = connection.getHeaderField("Location")
                    connection = URL(newUrl).openConnection() as HttpURLConnection
                }
            }

            val input = if (connection.responseCode < HttpURLConnection.HTTP_BAD_REQUEST) connection.inputStream else connection.errorStream

            input.use { _ ->
                val reader = BufferedReader(InputStreamReader(input, Charset.forName("UTF-8")))

                val text = StringBuilder()
                var cp: Int

                do {
                    cp = reader.read()
                    if (cp == -1) break

                    text.append(cp.toChar())
                } while (true)

                return JSONObject(text.toString())
            }
        }
    }

    interface ForecastCallback {
        fun onSuccess(model: ForecastResponseModel)
        fun onFail(message: String)
    }
}
