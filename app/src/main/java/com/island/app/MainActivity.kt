package com.island.app

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import com.island.app.service.IslandOverlayService
import com.island.app.service.IslandNotificationListener

class MainActivity : Activity() {

    private val OVERLAY_REQUEST_CODE = 1001
    private val NOTIFICATION_REQUEST_CODE = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<Button>(R.id.btnStartIsland).setOnClickListener {
            checkPermissionsAndStart()
        }
    }

    override fun onResume() {
        super.onResume()
        updateButtonState()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        checkPermissionsAndStart()
    }

    private fun updateButtonState() {
        val allGranted = hasOverlayPermission() && hasNotificationListenerPermission()
        findViewById<Button>(R.id.btnStartIsland).text =
            if (allGranted) getString(R.string.start_island)
            else getString(R.string.grant_permission)
    }

    private fun checkPermissionsAndStart() {
        if (!hasOverlayPermission()) {
            val intent = Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:$packageName")
            )
            @Suppress("DEPRECATION")
            startActivityForResult(intent, OVERLAY_REQUEST_CODE)
            return
        }
        if (!hasNotificationListenerPermission()) {
            val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
            @Suppress("DEPRECATION")
            startActivityForResult(intent, NOTIFICATION_REQUEST_CODE)
            return
        }
        startIslandService()
    }

    private fun hasOverlayPermission(): Boolean = Settings.canDrawOverlays(this)

    private fun hasNotificationListenerPermission(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val componentName = ComponentName(this, IslandNotificationListener::class.java)
        return enabledListeners.contains(componentName.flattenToString())
    }

    private fun startIslandService() {
        val intent = Intent(this, IslandOverlayService::class.java)
        startForegroundService(intent)
    }
}
