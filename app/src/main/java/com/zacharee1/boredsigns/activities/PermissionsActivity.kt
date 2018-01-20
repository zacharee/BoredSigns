package com.zacharee1.boredsigns.activities

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.zacharee1.boredsigns.fragments.WeatherFragment
import com.zacharee1.boredsigns.widgets.ImageWidget
import com.zacharee1.boredsigns.widgets.InfoWidget

class PermissionsActivity : AppCompatActivity() {
    companion object {
        val REQUEST = arrayOf(
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.ACCESS_FINE_LOCATION
        )

        val IMAGE_REQUEST = arrayOf(
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        )

        val INFO_REQUEST = arrayOf(
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )

        val WEATHER_REQUEST = arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    private lateinit var klass: Class<*>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        klass = intent.extras["class"] as Class<*>

        requestPermissions(REQUEST, 101)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        updateAll()
        finish()
    }

    fun updateAll() {
        val man = AppWidgetManager.getInstance(this)
        val ids = man.getAppWidgetIds(ComponentName(this, klass))
        val updateIntent = Intent()
        updateIntent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
        updateIntent.putExtra("appWidgetIds", ids)
        updateIntent.component = ComponentName(this, klass)
        sendBroadcast(updateIntent)
    }
}
