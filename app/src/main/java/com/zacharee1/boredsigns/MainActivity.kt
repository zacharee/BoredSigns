package com.zacharee1.boredsigns

import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import com.zacharee1.boredsigns.fragments.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        fragmentManager.beginTransaction().replace(R.id.content_main, MainFragment()).commit()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        val intent = Intent(Intent.ACTION_VIEW)
        when (item?.itemId) {
            R.id.play -> {
                intent.data = Uri.parse("https://play.google.com/store/apps/details?id=com.zacharee1.boredsigns")
            }

            R.id.github -> {
                intent.data = Uri.parse("https://github.com/zacharee/BoredSigns")
            }

            R.id.google_plus -> {
                intent.data = Uri.parse("https://plus.google.com/communities/105544332208886942595")
            }
        }

        startActivity(intent)
        return true
    }
}
