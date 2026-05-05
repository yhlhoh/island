package com.island.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.island.app.service.IslandOverlayService

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val serviceIntent = Intent(context, IslandOverlayService::class.java)
            context.startForegroundService(serviceIntent)
        }
    }
}
