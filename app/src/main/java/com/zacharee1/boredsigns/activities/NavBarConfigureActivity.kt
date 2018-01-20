package com.zacharee1.boredsigns.activities

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
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
import com.zacharee1.boredsigns.views.NavBarDragItemView
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
            view.findViewById<TextView>(R.id.text).text = button.getName()
            view.key = button.getKey()

            if (!addIfNotContains(view)) buttonArray.add(view)
        }

        buttonOrderer.setOnViewSwapListener { firstView, firstPosition, secondView, secondPosition ->
            Collections.swap(buttonArray, firstPosition, secondPosition)

            val new = getKeyOrder(buttonArray)
            saveNewOrder(new)
            sendUpdate(new)
        }

        buttonOrderer.setContainerScrollView(findViewById(R.id.buttons_order_wrapper))

        val listener = View.OnClickListener {
            var keyToAdd = ""
            when (it.id) {
                R.id.back -> keyToAdd = "back"
                R.id.home -> keyToAdd = "home"
                R.id.recents -> keyToAdd = "recents"
                R.id.split -> keyToAdd = "split"
                R.id.power -> keyToAdd = "power"
                R.id.qs -> keyToAdd = "qs"
                R.id.notifs -> keyToAdd = "notif"
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
            view.findViewById<TextView>(R.id.text).text = button.getName()
            view.key = button.getKey()

            if (!addIfNotContains(view)) buttonArray.add(view)

            val new = getKeyOrder(buttonArray)
            saveNewOrder(new)
            sendUpdate(new)
        }

        val control = findViewById<LinearLayout>(R.id.control_bar)

        for (i in 0 until control.childCount) {
            val view = control.getChildAt(i)

            if (view is ImageView) {
                view.setOnClickListener(listener)
            }
        }
    }

    private fun addIfNotContains(view: NavBarDragItemView): Boolean {
        val orderer = findViewById<DragLinearLayout>(R.id.buttons_order)
        for (i in 0 until orderer.childCount) {
            val v = orderer.getChildAt(i)

            if (v is NavBarDragItemView) {
                if (v.key == view.key) return true
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

    @SuppressLint("ViewConstructor")
    class NavBarButton(context: Context, private var key: String?) : LinearLayout(context) {
        private var layoutId = 0

        init {
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.MATCH_PARENT)
            (layoutParams as LinearLayout.LayoutParams).weight = 1F

            setLayoutIdAndInflate()
        }

        private fun setLayoutIdAndInflate() {
            when (key) {
                "home" -> layoutId = R.layout.navbar_home
                "recents" -> layoutId = R.layout.navbar_recents
                "back" -> layoutId = R.layout.navbar_back
                "power" -> layoutId = R.layout.navbar_power
                "qs" -> layoutId = R.layout.navbar_qs
                "split" -> layoutId = R.layout.navbar_splitscreen
                "notif" -> layoutId = R.layout.navbar_notifs
            }

            if (layoutId == 0) return

            inflate(context, layoutId, this)
        }

        fun getKey(): String? {
            return key
        }

        fun getName(): String {
            var which = 0
            when (key) {
                "home" -> which = R.string.home
                "recents" -> which = R.string.recents
                "back" -> which = R.string.back
                "power" -> which = R.string.power
                "qs" -> which = R.string.qs
                "split" -> which = R.string.splitscreen
                "notif" -> which = R.string.notifications
            }

            return if (which != 0) context.resources.getString(which) else ""
        }
    }
}
