package com.zacharee1.boredsigns.prefs

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.preference.Preference
import android.preference.PreferenceManager
import android.provider.MediaStore
import android.util.AttributeSet
import android.view.View
import android.widget.ImageView
import com.zacharee1.boredsigns.R
import java.io.InputStream

class ImagePreference(context: Context, attributeSet: AttributeSet) : Preference(context, attributeSet) {
    private lateinit var view: View

    private var drawable: Drawable = context.resources.getDrawable(R.drawable.example, null)

    init {
        layoutResource = R.layout.image_preference

        val a = context.theme.obtainStyledAttributes(
                attributeSet,
                R.styleable.ImagePreference,
                0, 0)

        try {

        } finally {
            a.recycle()
        }

        val uriString = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            try {
                drawable = Drawable.createFromStream(context.contentResolver.openInputStream(uri), uri.toString())
            } catch (e: Exception) {
                drawable = context.resources.getDrawable(R.drawable.example, null)
                preferenceManager?.sharedPreferences?.edit()?.putString(key, null)?.apply()
            }
        }
    }

    override fun onBindView(view: View) {
        super.onBindView(view)
        this.view = view

        setDrawable(drawable)
    }

    fun refreshDrawable() {
        val uriString = PreferenceManager.getDefaultSharedPreferences(context).getString(key, null)
        if (uriString != null) {
            val uri = Uri.parse(uriString)
            drawable = Drawable.createFromStream(context.contentResolver.openInputStream(uri), uri.toString())
        }

        setDrawable(drawable)
    }

    fun setDrawable(drawable: Drawable) {
        this.drawable = drawable

        try {
            view.findViewById<ImageView>(R.id.image).setImageDrawable(drawable)
        } catch (e: Exception) {}
    }

    fun setDrawableByUri(uri: Uri) {
        drawable = Drawable.createFromStream(context.contentResolver.openInputStream(uri), uri.toString())

        setDrawable(drawable)
    }

    fun setResource(resource: Int) {
        drawable = context.resources.getDrawable(resource, null)

        setDrawable(drawable)
    }

    fun getDrawable(): Drawable {
        return drawable
    }
}