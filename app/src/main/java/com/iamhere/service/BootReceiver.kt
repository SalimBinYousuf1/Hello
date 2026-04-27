package com.iamhere.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val enabled = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
                .getBoolean("auto_start", true)
            if (enabled) {
                context.startForegroundService(Intent(context, MeshForegroundService::class.java))
            }
        }
    }
}
