package com.nuvia.ios.notifications

import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NuviaNotificationListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        super.onNotificationPosted(sbn)

        if (sbn.packageName == packageName) return

        val prefs = getSharedPreferences("nuvia_settings", MODE_PRIVATE)
        if (!prefs.getBoolean("island_enabled", true)) return

        val notification = sbn.notification ?: return
        val extras = notification.extras
        val title = extras.getCharSequence("android.title")?.toString().orEmpty()
        val body = (
            extras.getCharSequence("android.bigText")
                ?: extras.getCharSequence("android.text")
            )?.toString().orEmpty()

        val appName = runCatching {
            val appInfo = packageManager.getApplicationInfo(sbn.packageName, 0)
            packageManager.getApplicationLabel(appInfo).toString()
        }.getOrDefault(sbn.packageName.substringAfterLast('.'))

        val message = when {
            title.isNotBlank() && body.isNotBlank() -> "$title · $body"
            title.isNotBlank() -> title
            body.isNotBlank() -> body
            else -> "New notification"
        }

        IslandOverlay.show(this, appName, message)
    }

    override fun onNotificationRemoved(sbn: StatusBarNotification) {
        super.onNotificationRemoved(sbn)
    }
}
