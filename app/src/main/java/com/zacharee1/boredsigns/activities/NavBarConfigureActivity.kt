package com.zacharee1.boredsigns.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.preference.PreferenceManager
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.helper.ItemTouchHelper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import com.zacharee1.boredsigns.R
import com.zacharee1.boredsigns.widgets.NavBarWidget
import android.support.v7.widget.helper.ItemTouchHelper.Callback.makeFlag
import android.text.TextUtils
import android.view.DragEvent
import android.widget.Button
import android.widget.TextView
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.zacharee1.boredsigns.util.Utils
import com.zacharee1.boredsigns.views.NavBarButton
import com.zacharee1.boredsigns.views.NavBarDragItemView
import kotlinx.android.synthetic.main.aod_item.view.*
import java.util.*
import kotlin.collections.ArrayList

class NavBarConfigureActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_nav_bar_configure)

        val prefs = PreferenceManager.getDefaultSharedPreferences(this)
        val buttonsOrder = prefs.getString(NavBarWidget.BUTTONS_ORDER, NavBarWidget.DEFAULT_ORDER).split("|")

        updateNavBarPreview(buttonsOrder)

        val buttonOrderer = findViewById<DragLinearLayout>(R.id.buttons_order)
        val buttonArray = ArrayList<NavBarDragItemView>()

        buttonsOrder.forEach {
            val button = NavBarButton(this, it)
            val view = LayoutInflater.from(this).inflate(R.layout.vh, buttonOrderer, false) as NavBarDragItemView
            view.findViewById<ImageView>(R.id.close_button).setOnClickListener {
                buttonArray.remove(view)
                buttonOrderer.removeDragView(view)

                val new = getKeyOrder(buttonArray)
                saveNewOrder(new)
                sendUpdate(new)
            }
            view.findViewById<TextView>(R.id.text).text = button.name
            view.key = button.key

            if (!addIfNotContains(view)) buttonArray.add(view)
        }

        buttonOrderer.setOnViewSwapListener { _, firstPosition, _, secondPosition ->
            Collections.swap(buttonArray, firstPosition, secondPosition)

            val new = getKeyOrder(buttonArray)
            saveNewOrder(new)
            sendUpdate(new)
        }

        buttonOrderer.setContainerScrollView(findViewById(R.id.buttons_order_wrapper))

        val listener = View.OnClickListener {
            var keyToAdd = ""
            when (it.id) {
                R.id.back -> keyToAdd = NavBarButton.BACK
                R.id.home -> keyToAdd = NavBarButton.HOME
                R.id.recents -> keyToAdd = NavBarButton.RECENTS
                R.id.split -> keyToAdd = NavBarButton.SPLIT
                R.id.power -> keyToAdd = NavBarButton.POWER
                R.id.qs -> keyToAdd = NavBarButton.QS
                R.id.notifs -> keyToAdd = NavBarButton.NOTIF
                R.id.assist -> keyToAdd = NavBarButton.ASSIST
            }

            val button = NavBarButton(this, keyToAdd)
            val view = LayoutInflater.from(this).inflate(R.layout.vh, buttonOrderer, false) as NavBarDragItemView
            view.findViewById<ImageView>(R.id.close_button).setOnClickListener {
                buttonArray.remove(view)
                buttonOrderer.removeDragView(view)

                val new = getKeyOrder(buttonArray)
                saveNewOrder(new)
                sendUpdate(new)
            }
            view.findViewById<TextView>(R.id.text).text = button.name
            view.key = button.key

            if (!addIfNotContains(view)) buttonArray.add(view)

            val new = getKeyOrder(buttonArray)
            saveNewOrder(new)
            sendUpdate(new)
        }

        val control = findViewById<LinearLayout>(R.id.control_bar)

        (0 until control.childCount)
                .map { control.getChildAt(it) }
                .filterIsInstance<ImageView>()
                .forEach { it.setOnClickListener(listener) }
    }

    private fun addIfNotContains(view: NavBarDragItemView): Boolean {
        val orderer = findViewById<DragLinearLayout>(R.id.buttons_order)
        for (i in 0 until orderer.childCount) {
            val v = orderer.getChildAt(i)

            if (v is NavBarDragItemView) {
                if (v.key == view.key) return true
            }
        }

        view.findViewById<ImageView>(R.id.choose_image)?.let{
            it.setOnClickListener {
                val intent = Intent(this, ImagePickerActivity::class.java)
                intent.data = Uri.parse("2")
                intent.putExtra("key", view.key)
                startActivity(intent)
            }
            it.setOnLongClickListener {
                PreferenceManager.getDefaultSharedPreferences(this).edit().putString(view.key, null).apply()
                Utils.sendWidgetUpdate(this, NavBarWidget::class.java, null)
                true
            }
        }

        orderer.addDragView(view, view.findViewById(R.id.drag_handle))
        return false
    }

    private fun getKeyOrder(buttons: List<NavBarDragItemView>): List<String?> {
        val keys = ArrayList<String?>()

        buttons.forEach {
            keys.add(it.key)
        }

        return keys
    }

    private fun saveNewOrder(keys: List<String?>) {
        val string = TextUtils.join("|", keys)
        val prefs = PreferenceManager.getDefaultSharedPreferences(this)

        prefs.edit().putString(NavBarWidget.BUTTONS_ORDER, string).apply()
    }

    private fun sendUpdate(newOrder: List<String?>) {
        updateNavBarPreview(newOrder)
        Utils.sendWidgetUpdate(this, NavBarWidget::class.java, null)
    }

    private fun updateNavBarPreview(order: List<String?>) {
        val navBar = findViewById<LinearLayout>(R.id.navbar_view)
        navBar.removeAllViews()

        order.forEach {
            val view = NavBarButton(this, it)
            navBar.addView(view)
        }
    }
}
