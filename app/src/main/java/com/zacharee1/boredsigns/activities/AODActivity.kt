package com.zacharee1.boredsigns.activities

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.net.Uri
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.SparseArray
import android.view.LayoutInflater
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import com.jmedeisis.draglinearlayout.DragLinearLayout
import com.zacharee1.boredsigns.R
import java.util.*
import kotlin.collections.ArrayList

class AODActivity : AppCompatActivity() {
    companion object {
        private lateinit var appWidgetManager: AppWidgetManager
        private lateinit var appWidgetHost: AppWidgetHost

        private val aodWidgetTable: Hashtable<ComponentName, AppWidgetProviderInfo> = Hashtable()

        private fun getProviderInfo(info: WidgetInfo): AppWidgetProviderInfo? {
            val providerInfo = appWidgetManager.getAppWidgetInfo(info.widgetId)
            if (providerInfo != null && (providerInfo.provider == null || providerInfo.provider == ComponentName(info.packageName, info.className))) {
                return providerInfo
            }

            return aodWidgetTable[ComponentName(info.packageName, info.className)]
        }
    }

    private lateinit var signBoardContext: Context

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aod)

        signBoardContext = createPackageContext("com.lge.signboard", 0)

        appWidgetManager = AppWidgetManager.getInstance(signBoardContext)
        appWidgetHost = AppWidgetHost(signBoardContext, 4660)

        val getInstalledProviders = appWidgetManager::class.java.getMethod("getInstalledProviders", Int::class.java)
        val providers = getInstalledProviders.invoke(appWidgetManager, 0x9000) as MutableList<*>

        for (provider in providers) {
            provider as AppWidgetProviderInfo
            aodWidgetTable[provider.provider] = provider
        }

        setupDrag()
    }

    private fun setupDrag() {
        val dragView = findViewById<DragLinearLayout>(R.id.order_view)
        val dragScroller = findViewById<ScrollView>(R.id.order_scroller)

        dragView.setContainerScrollView(dragScroller)

        val currentWidgets = parseCurrentWidgets()
        val availWidgets = parseAvailWidgets()
        sortAvailWidgets(availWidgets, currentWidgets)

        for (widget in availWidgets) {
            if (widget.position == Integer.MAX_VALUE) widget.position = availWidgets.indexOf(widget)

            val view = LayoutInflater.from(this).inflate(R.layout.aod_item, dragView, false)
            view.findViewById<TextView>(R.id.name).text = widget.label
            view.findViewById<Switch>(R.id.toggle).let {
                it.isChecked = currentWidgets.contains(widget)
                it.setOnCheckedChangeListener { _, b ->
                    if (b) {
                        if (!currentWidgets.contains(widget)) {
                            currentWidgets.add(widget)
                            dragView.removeDragView(view)
                            dragView.addDragView(view, view.findViewById(R.id.drag_handle), currentWidgets.size - 1)
                        }
                    } else {
                        currentWidgets.remove(widget)
                        removeWidget(widget)
                        dragView.removeDragView(view, view.findViewById(R.id.drag_handle), false)
                        dragView.addView(view, currentWidgets.size)
                    }
                    saveNewOrder(currentWidgets)
                }
            }
            view.setOnClickListener {
                val config = getProviderInfo(widget)?.configure
                if (config != null && config.className.isNotEmpty()) {
                    val intent = Intent(Intent.ACTION_VIEW)
                    intent.component = config
                    try {
                        startActivity(intent)
                    } catch (e: Exception) {
                        Toast.makeText(this@AODActivity, resources.getText(R.string.error_launching_config), Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AODActivity, resources.getText(R.string.no_config_activity), Toast.LENGTH_SHORT).show()
                }
            }
            if (currentWidgets.contains(widget)) dragView.addDragView(view, view.findViewById(R.id.drag_handle))
            else dragView.addView(view)
        }

        dragView.setOnViewSwapListener { _, firstPosition, _, secondPosition ->
            if (currentWidgets.size > firstPosition
                    && currentWidgets.size > secondPosition
                    && (currentWidgets[firstPosition].position != Int.MAX_VALUE
                            && currentWidgets[secondPosition].position != Int.MAX_VALUE)) {
                currentWidgets[firstPosition].position = secondPosition
                currentWidgets[secondPosition].position = firstPosition
                Collections.swap(currentWidgets, firstPosition, secondPosition)
                saveNewOrder(currentWidgets)
            }
        }
    }

    private fun parseCurrentWidgets(): ArrayList<WidgetInfo> {
        val queryUri = Uri.parse("content://com.lge.provider.signboard/appwidget?notify=true")
        val c = signBoardContext.contentResolver.query(queryUri, null, "screenStatus = 'off'", null, "position")
        val ret = ArrayList<WidgetInfo>()

        c.use {
            if (it != null) {
                val idIndex = it.getColumnIndexOrThrow("id")
                val widgetIdIndex = it.getColumnIndexOrThrow("appWidgetId")
                val positionIndex = it.getColumnIndexOrThrow("position")
                val packageIndex = it.getColumnIndexOrThrow("packageName")
                val classIndex = it.getColumnIndexOrThrow("className")
                val enableIndex = it.getColumnIndexOrThrow("enable")

                while (it.moveToNext()) {
                    val info = WidgetInfo()
                    info.id = it.getInt(idIndex)
                    info.widgetId = it.getInt(widgetIdIndex)
                    info.position = it.getInt(positionIndex)
                    info.packageName = it.getString(packageIndex)
                    info.className = it.getString(classIndex)
                    info.enable = it.getInt(enableIndex) != 0
                    info.label = getProviderInfo(info)?.loadLabel(packageManager) ?: "Null"

                    ret.add(info)
                }
            }
        }
        return ret
    }

    private fun parseAvailWidgets(): ArrayList<WidgetInfo> {

        val ret = ArrayList<WidgetInfo>()

        for (provider in aodWidgetTable.values) {
            val info = WidgetInfo()
            info.packageName = provider.provider.packageName
            info.className = provider.provider.className
            info.label = provider.loadLabel(packageManager)
            ret.add(info)
        }

        return ret
    }

    private fun sortAvailWidgets(avail: ArrayList<WidgetInfo>, current: ArrayList<WidgetInfo>) {
        for (widget in avail) {
            if (current.contains(widget)) {
                val c = current[current.indexOf(widget)]
                widget.position = c.position
                widget.id = c.id
                widget.widgetId = c.widgetId
            }
        }

        Collections.sort(avail)
    }

    private fun saveNewOrder(list: ArrayList<WidgetInfo>) {
        for (info in list) {
            saveWidget(info)
        }
    }

    private fun saveWidget(info: WidgetInfo) {
        val queryUri = Uri.parse("content://com.lge.provider.signboard/appwidget?notify=true")

        if (info.position != Int.MAX_VALUE && info.id != -1) {
            signBoardContext.contentResolver.update(queryUri, info.toContentValues(), "id=" + info.id, null)
        } else {
            info.position = getMaxOrder()
            if (info.id == -1 && !parseCurrentWidgets().contains(info)) {
                info.id = ContentUris.parseId(signBoardContext.contentResolver.insert(queryUri, info.toContentValues())).toInt()
            }
        }
    }

    private fun removeWidget(info: WidgetInfo) {
        val queryUri = Uri.parse("content://com.lge.provider.signboard/appwidget?notify=true")

        signBoardContext.contentResolver.delete(queryUri, "id=?", Array(1, {info.id.toString()}))
    }

    private fun getMaxOrder(): Int {
        val c = signBoardContext.contentResolver.query(Uri.parse("content://com.lge.provider.signboard/appwidget?notify=true"), arrayOf("MAX(position)"), null, null, null)
        var maxOrder = 0
        c?.use {
            c.moveToLast()
            maxOrder = c.getInt(0)
        }

        return maxOrder + 1
    }

    class WidgetInfo : Comparable<WidgetInfo> {
        var id = -1
        var widgetId = 0
        var position = Int.MAX_VALUE
        var packageName = ""
        var className = ""
        var enable = false
        var label = ""

        fun toContentValues(): ContentValues {
            val ret = ContentValues()
            ret.put("screenStatus", "off")
            ret.put("packageName", packageName)
            ret.put("className", className)
            ret.put("appWidgetId", widgetId)
            ret.put("enable", if (enable) 1 else 0)
            ret.put("position", position)

            return ret
        }

        override fun compareTo(other: WidgetInfo): Int {
            return Integer.compare(position, other.position)
        }

        override fun toString(): String {
            return "WidgetInfo { id: $id, widgetId: $widgetId, position: $position, packageName: $packageName, className: $className, enable: $enable, label: $label }"
        }

        override fun equals(other: Any?): Boolean {
            other as WidgetInfo?

            return if (other == null) false else other.packageName == packageName && other.className == className
        }

        override fun hashCode(): Int {
            return id + widgetId + position + className::class.java.hashCode() + packageName::class.java.hashCode()
        }
    }
}
