package com.zacharee1.boredsigns.services

import android.accessibilityservice.AccessibilityService
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.support.v4.content.LocalBroadcastManager
import android.view.accessibility.AccessibilityEvent

class NavBarAccessibility : AccessibilityService() {
    companion object {
        val BASE = "com.zacharee1.boredsigns.action."
        val HOME = BASE + "HOME"
        val BACK = BASE + "BACK"
        val RECENTS = BASE + "RECENTS"
        val NOTIFS = BASE + "NOTIFS"
        val SPLIT = BASE + "SPLIT"
        val QS = BASE + "QS"
        val POWER = BASE + "POWER"
    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context, p1: Intent?) {
            val statusBarManager = p0.getSystemService("statusbar")
            val collapsePanels = Class.forName("android.app.StatusBarManager").getMethod("collapsePanels")

            when (p1?.action) {
                RECENTS -> performGlobalAction(GLOBAL_ACTION_RECENTS)

                HOME -> performGlobalAction(GLOBAL_ACTION_HOME)

                BACK -> performGlobalAction(GLOBAL_ACTION_BACK)

                NOTIFS -> {
                    collapsePanels.invoke(statusBarManager)
                    performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS)
                }

                SPLIT -> performGlobalAction(GLOBAL_ACTION_TOGGLE_SPLIT_SCREEN)

                QS -> {
                    collapsePanels.invoke(statusBarManager)
                    performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS)
                }

                POWER -> performGlobalAction(GLOBAL_ACTION_POWER_DIALOG)
            }
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo.eventTypes = 0

        val filter = IntentFilter()
        filter.addAction(HOME)
        filter.addAction(RECENTS)
        filter.addAction(BACK)
        filter.addAction(NOTIFS)
        filter.addAction(SPLIT)
        filter.addAction(QS)
        filter.addAction(POWER)

        registerReceiver(receiver, filter)
    }

    override fun onAccessibilityEvent(p0: AccessibilityEvent?) {

    }

    override fun onInterrupt() {

    }

    override fun onDestroy() {
        super.onDestroy()

        unregisterReceiver(receiver)
    }
}
