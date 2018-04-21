package com.zacharee1.boredsigns.fragments

import android.app.AlertDialog
import android.content.ComponentName
import android.content.Intent
import android.os.Bundle
import android.preference.PreferenceFragment
import android.widget.Toast
import com.zacharee1.boredsigns.R

class MainFragment : PreferenceFragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        addPreferencesFromResource(R.xml.prefs_main)

        val openScreenOn = findPreference("screen_on")
        openScreenOn.setOnPreferenceClickListener {
            try {
                val cmp = ComponentName("com.lge.signboard.settings", "com.lge.signboard.settings.SBSettingContents")
                val intent = Intent(Intent.ACTION_MAIN)
                intent.component = cmp

                startActivity(intent)
            } catch (e: Exception) {
                AlertDialog.Builder(context)
                        .setTitle(R.string.error_launching_config)
                        .setMessage(R.string.unable_to_directly_launch_settings)
                        .setPositiveButton(android.R.string.ok, { _, _ ->
                            try {
                                startActivity(Intent("com.lge.signboard.mainSettings"))
                            } catch (e: Exception) {
                                Toast.makeText(context, resources.getText(R.string.error_launching_config), Toast.LENGTH_SHORT).show()
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show()
            }

            true
        }
    }
}