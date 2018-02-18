package com.zacharee1.boredsigns.activities

import android.app.AlertDialog
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.zacharee1.boredsigns.R

class NotSupportedActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AlertDialog.Builder(this)
                .setTitle(R.string.not_compat_title)
                .setMessage(R.string.not_compat_message)
                .setPositiveButton(android.R.string.ok, { _, _ ->
                    System.exit(0)
                })
                .show()
    }
}
