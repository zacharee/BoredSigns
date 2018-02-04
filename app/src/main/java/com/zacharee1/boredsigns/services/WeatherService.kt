package com.zacharee1.boredsigns.services

import android.Manifest
import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.app.Service
import android.appwidget.AppWidgetManager
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
import android.widget.Toast
import com.google.android.gms.location.*
import com.google.firebase.analytics.FirebaseAnalytics
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.proxies.WeatherProxy
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.InfoWidget
import com.zacharee1.boredsigns.widgets.WeatherForecastWidget
import com.zacharee1.boredsigns.widgets.WeatherWidget
import github.vatsal.easyweather.Helper.ForecastCallback
import github.vatsal.easyweather.Helper.TempUnitConverter
import github.vatsal.easyweather.Helper.WeatherCallback
import github.vatsal.easyweather.WeatherMap
import github.vatsal.easyweather.retrofit.models.ForecastResponseModel
import github.vatsal.easyweather.retrofit.models.WeatherResponseModel
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.apache.commons.text.WordUtils
import java.net.HttpURLConnection
import java.net.URL
import java.text.DecimalFormat
import java.util.*
import kotlin.collections.ArrayList

class WeatherService : Service() {
    companion object {
        const val ACTION_UPDATE_WEATHER = "com.zacharee1.boredsigns.action.UPDATE_WEATHER"

        const val EXTRA_TEMP = "temp"
        const val EXTRA_LOC = "loc"
        const val EXTRA_DESC = "desc"
        const val EXTRA_ICON = "icon"

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
        prefs.unregisterOnSharedPreferenceChangeListener(prefsListener)
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver)
        alarmManager.cancel(alarmIntent)
    }

    private fun onHandleIntent(action: String?) {
        when (action) {
            ACTION_UPDATE_WEATHER -> {
                useCelsius = prefs.getBoolean(WHICH_UNIT, true)

                if (apiKey == null) {
                    val intent = Intent(this, WeatherProxy::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    startActivity(intent)

                    Toast.makeText(this, resources.getText(R.string.add_owm_key), Toast.LENGTH_SHORT).show()
                } else {
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
    }

    private fun getWeather(lat: Double, lon: Double) {
        try {
            val geo = Geocoder(applicationContext, Locale.getDefault())
            val weather = WeatherMap(applicationContext, apiKey)
            val addrs = geo.getFromLocation(lat, lon, 1)

            if (isCurrentActivated()) {
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

            if (isForecastActivated()) {
                weather.getLocationForecast(lat.toString(), lon.toString(), object : ForecastCallback() {
                    @SuppressLint("CheckResult")
                    override fun success(model: ForecastResponseModel) {
                        val extras = Bundle()

                        val temps: ArrayList<String> = ArrayList()
                        val descs: ArrayList<String> = ArrayList()

                        model.list.mapTo(descs) { WordUtils.capitalize(it.weather[0].description) }

                        model.list
                                .map { it.main.temp }
                                .map { if (useCelsius) TempUnitConverter.convertToCelsius(it) else TempUnitConverter.convertToFahrenheit(it) }
                                .map { DecimalFormat("#").format(it).toString() }
                                .mapTo(temps) { it + "°" + if (useCelsius) "C" else "F" }

                        extras.putStringArrayList(EXTRA_TEMP, temps)
                        extras.putStringArrayList(EXTRA_DESC, descs)
                        extras.putString(EXTRA_LOC, addrs[0].locality + ", " + addrs[0].adminArea)

                        Utils.sendWidgetUpdate(this@WeatherService, WeatherForecastWidget::class.java, extras)

                        var urls = ArrayList<URL>()
                        model.list.mapTo(urls) { URL(it.weather[0].iconLink) }

                        urls = ArrayList(urls.subList(0, Math.min(urls.size, 5)))

                        Observable.fromCallable({asyncLoadUrls(urls)})
                                .subscribeOn(Schedulers.io())
                                .observeOn(AndroidSchedulers.mainThread())
                                .subscribe {
                                    list ->
                                    extras.putParcelableArrayList(EXTRA_ICON, list)
                                    Utils.sendWidgetUpdate(this@WeatherService, WeatherForecastWidget::class.java, extras)
                                }
                    }

                    override fun failure(error: String?) {
                        Toast.makeText(this@WeatherService, String.format(Locale.US, resources.getString(R.string.error_retrieving_weather), error), Toast.LENGTH_SHORT).show()
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

    private fun asyncLoadUrl(url: URL): Bitmap {
        return try {
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            BitmapFactory.decodeStream(connection.inputStream) ?: throw Exception()
        } catch (e: Exception) {
            BitmapFactory.decodeResource(resources, R.drawable.ic_wb_sunny_black_24dp)
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
        val man = AppWidgetManager.getInstance(this)
        val ids = man.getAppWidgetIds(ComponentName(this, WeatherWidget::class.java))

        return ids != null && ids.isNotEmpty()
    }

    private fun isForecastActivated(): Boolean {
        val man = AppWidgetManager.getInstance(this)
        val ids = man.getAppWidgetIds(ComponentName(this, WeatherForecastWidget::class.java))

        return ids != null && ids.isNotEmpty()
    }
}
