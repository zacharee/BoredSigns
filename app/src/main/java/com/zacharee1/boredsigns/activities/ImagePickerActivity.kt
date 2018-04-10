package com.zacharee1.boredsigns.activities

import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.preference.PreferenceManager
import android.support.v7.app.AppCompatActivity
import android.util.Base64
import com.theartofdev.edmodo.cropper.CropImage
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.widgets.ImageWidget
import com.zacharee1.boredsigns.widgets.NavBarWidget
import java.io.ByteArrayOutputStream

class ImagePickerActivity : AppCompatActivity() {
    companion object {
        const val SELECT_PICTURE = 1
        const val SELECT_NAV_ICON = 2
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_picker)

        val intent = Intent()
        intent.type = "image/*"
        intent.action = Intent.ACTION_GET_CONTENT
        startActivityForResult(Intent.createChooser(intent, resources.getText(R.string.select_image)), ImagePickerActivity@this.intent.data.toString().toInt())
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            val result = CropImage.getActivityResult(data)

            if (resultCode == Activity.RESULT_OK) {
                val uri = result.uri
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString("image_picker", uri.toString()).apply()
                Utils.sendWidgetUpdate(this, ImageWidget::class.java, null)
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
        } else if (requestCode == SELECT_NAV_ICON) {
            val uri = data?.data
            uri?.let {
                val bmp = Utils.getResizedBitmap(BitmapFactory.decodeStream(contentResolver.openInputStream(uri)), 100, 100)
                val stream = ByteArrayOutputStream()

                bmp.compress(Bitmap.CompressFormat.PNG, 100, stream)
                val array = stream.toByteArray()
                val encoded = Base64.encodeToString(array, Base64.DEFAULT)

                PreferenceManager.getDefaultSharedPreferences(this).edit().putString(intent.getStringExtra("key"), encoded).apply()
                Utils.sendWidgetUpdate(this, NavBarWidget::class.java, null)
            }
            finish()
        }
    }
}
