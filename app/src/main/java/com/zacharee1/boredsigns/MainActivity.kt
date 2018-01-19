package com.zacharee1.boredsigns

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.zacharee1.boredsigns.fragments.MainFragment

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity)

        fragmentManager.beginTransaction().replace(R.id.content_main, MainFragment()).commit()
    }
}
