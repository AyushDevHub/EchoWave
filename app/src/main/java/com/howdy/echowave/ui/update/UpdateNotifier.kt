package com.howdy.echowave.ui.update

import android.annotation.SuppressLint
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.howdy.echowave.R
import com.howdy.echowave.data.remote.update.LatestAppRelease

/** Local, on-open release notice. No device token or usage event is sent. */
object UpdateNotifier {
    private const val CHANNEL_ID = "app_updates"
    private const val PREFS = "update_notifications"
    private const val LAST_VERSION = "last_notified_version"
    private const val NOTIFICATION_ID = 101

    fun permissionGranted(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    // permissionGranted() checks POST_NOTIFICATIONS immediately before this call.
    @SuppressLint("MissingPermission")
    fun notifyIfAllowed(context: Context, release: LatestAppRelease) {
        if (!permissionGranted(context)) return
        val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (preferences.getString(LAST_VERSION, null) == release.version) return

        val manager = context.getSystemService(android.app.NotificationManager::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                android.app.NotificationChannel(
                    CHANNEL_ID,
                    "App updates",
                    android.app.NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = "A notice when a newer EchoWave release is available" },
            )
        }
        val openRelease = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(Intent.ACTION_VIEW, Uri.parse(release.releasePageUrl)),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("EchoWave update available")
            .setContentText("Version ${release.version} is ready. Tap to view the release.")
            .setContentIntent(openRelease)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification) }
            .onSuccess { preferences.edit().putString(LAST_VERSION, release.version).apply() }
    }
}
