package com.island.app.service

import android.app.Notification
import android.graphics.drawable.Icon
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class IslandNotificationListener : NotificationListenerService() {

    interface NotificationCallback {
        fun onNotificationPosted(data: NotificationData)
        fun onNotificationRemoved()
    }

    data class NotificationData(
        val title: String,
        val text: String,
        val icon: Icon?,
        val actions: List<Notification.Action>,
        val key: String
    )

    companion object {
        private var callback: NotificationCallback? = null
        private var instance: IslandNotificationListener? = null

        fun setCallback(cb: NotificationCallback?) {
            callback = cb
        }

        fun cancelNotification(key: String) {
            instance?.cancelNotification(key)
        }
    }

    override fun onListenerConnected() {
        instance = this
    }

    override fun onListenerDisconnected() {
        instance = null
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        val notification = sbn.notification ?: return

        // Skip media notifications
        if (notification.extras.containsKey(Notification.EXTRA_MEDIA_SESSION)) return
        // Skip ongoing notifications
        if (notification.flags and Notification.FLAG_ONGOING_EVENT != 0) return

        val extras = notification.extras
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString() ?: return
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: ""
        val smallIcon = notification.smallIcon

        val actionObjects = notification.actions?.take(2)?.toList() ?: emptyList()

        val data = NotificationData(
            title = title,
            text = text,
            icon = smallIcon,
            actions = actionObjects,
            key = sbn.key
        )
        callback?.onNotificationPosted(data)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        callback?.onNotificationRemoved()
    }
}
