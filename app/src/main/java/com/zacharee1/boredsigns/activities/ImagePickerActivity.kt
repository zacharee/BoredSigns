package com.zacharee1.boredsigns.activities

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.preference.PreferenceManager
import com.theartofdev.edmodo.cropper.CropImage
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.services.InfoService
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.ImageWidget
import com.zacharee1.boredsigns.widgets.InfoWidget

class ImagePickerActivity : AppCompatActivity() {
    companion object {
        val SELECT_PICTURE = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, resources.getText(R.string.select_image)), SELECT_PICTURE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                val uri = result.uri
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("image_picker", uri.toString()).apply()
                Utils.sendWidgetUpdate(this, ImageWidget::class.java)
                finish()
            }
        } else if (requestCode == SELECT_PICTURE) {
            val uri = data?.data
            if (uri != null) {
                CropImage.activity(uri)
                        .setAspectRatio(1040, 160)
                        .start(this)
            } else {
                finish()
            }
        }
    }
}
