package com.zacharee1.boredsigns.util

import android.content.Context
import android.graphics.Typeface

object WeatherFonts {
    private const val WEATHER_PATH = "fonts/weathericons-regular-webfont.ttf"

    fun getWeather(context: Context): Typeface {
        return Typeface.createFromAsset(context.assets, WEATHER_PATH)
    }
}