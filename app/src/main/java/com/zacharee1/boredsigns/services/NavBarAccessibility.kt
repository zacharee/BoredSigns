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
    }

    val receiver = object : BroadcastReceiver() {
        override fun onReceive(p0: Context?, p1: Intent?) {
            when (p1?.action) {
                RECENTS -> {
                    performGlobalAction(GLOBAL_ACTION_RECENTS)
                }

                HOME -> {
                    performGlobalAction(GLOBAL_ACTION_HOME)
                }

                BACK -> {
                    performGlobalAction(GLOBAL_ACTION_BACK)
                }
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
