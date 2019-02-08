package com.lrsus.venusdkdemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.support.v4.app.NotificationCompat
import android.support.v4.app.NotificationManagerCompat
import com.lrsus.venusdk.VENUDiscoveryReceiver
import java.util.*

/**
 * Extending VENUDiscoveryReceiver enables an Application to respond to BLE advertisements
 * coming from the VENU system from the background. In this example, we are only starting a
 * notification in order to spur the user to open the app. They are free to ignore the notification,
 * especially in the case that they are only passing by the location in question.
 *
 */
class MainApplication : VENUDiscoveryReceiver() {

    companion object {
        private var activityRunning : Boolean = false

        @JvmStatic
        val TAG = "VENUDEMO"

        @JvmStatic
        val APP_NAMESPACE = "com.lrsus.venudemo"

        @JvmStatic
        fun isActivityRunning() : Boolean {
            return activityRunning
        }

        @JvmStatic
        fun setActivityRunStatus(isRunning : Boolean) {
            activityRunning = isRunning
        }
    }

    override fun onCreate() {
        super.onCreate()

        // Establish notification channel for Android 8.0+
        createNotificationChannel()
    }

    // Copied from Android documentation:
    // https://developer.android.com/training/notify-user/build-notification
    private fun createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.channel_name)
            val descriptionText = getString(R.string.channel_description)
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(APP_NAMESPACE, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    override fun venuDiscovered(locationId : Int?) {
        // MainActivity intent
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent
                .getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT)

        // Set up notification
        val notification = NotificationCompat.Builder(this, APP_NAMESPACE)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle("Hi! Are you near a venue?")
                .setContentText("Tap here to open the VENU app!")
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build()

        if (!isActivityRunning()) {
            with (NotificationManagerCompat.from(this)) {
                notify(26234, notification)
            }
        }
    }

    override fun appNamespace(): String {
        return APP_NAMESPACE
    }

    /**
     * Brand ID will match the iBeacon UUID when discovering.
     */
    override fun brandId(): UUID {
//        return UUID.fromString("f05b1f5a-646e-4169-b0c6-a50b84c8b624")
        return UUID.fromString("671d3a8e-ee94-4395-9177-d5382d75ff10")
    }

}