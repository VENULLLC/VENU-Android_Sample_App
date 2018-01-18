/**
 * BroadcastService.kt
 * Copyright 2018 Long Range Systems, LLC
 */
package com.lrsus.lrsradiusdemo

import com.lrsus.venusdk.VenuBroadcast
import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import android.app.PendingIntent
import android.os.Binder
import android.support.v4.app.NotificationCompat


class BroadcastService : Service() {

    private val mBinder : IBinder = BroadcastServiceBinder()
    private val LOG_TAG : String = "BroadcastService"
    private val mOrgUUID: String = "5241444e-5441-424c-4553-455256494345"
    private var venuBroadcast : VenuBroadcast? = null
    var serviceNumber : Int? = null

    companion object {
        var IS_SERVICE_RUNNING : Boolean = false
    }

    inner class BroadcastServiceBinder : Binder() {
        fun getService() : BroadcastService {
            return this@BroadcastService
        }
    }

    override fun onCreate() {
        if (venuBroadcast == null) {
            venuBroadcast = VenuBroadcast(applicationContext, mOrgUUID, 2)
        }
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action.equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent ")
            showNotification()
            Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show()
            venuBroadcast?.start()
            serviceNumber = venuBroadcast?.serviceNumber
        } else if (intent?.action.equals(Constants.ACTION.BROADCAST_ACTION)) {
            Log.i(LOG_TAG, "Clicked Broadcast");
            Toast.makeText(this, "Clicked Broadcast!", Toast.LENGTH_SHORT).show()
        } else if (intent?.action.equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent")
            venuBroadcast?.stop()
            stopForeground(true)
            stopSelf()
        }
        return START_STICKY
    }

    private fun showNotification() {
        val notificationIntent = Intent(this, MainActivity::class.java)
        notificationIntent.action = Constants.ACTION.MAIN_ACTION
        notificationIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        val pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, 0)

        val broadcastIntent = Intent(this, BroadcastService::class.java)
        broadcastIntent.action = Constants.ACTION.BROADCAST_ACTION
        val broadcastIntentService = PendingIntent.getService(this, 0, broadcastIntent, 0)

//        val icon = BitmapFactory.decodeResource(resources,
//                R.drawable.ic_launcher)

        val notification = NotificationCompat.Builder(this)
                .setContentTitle("LRSRadiusDemo Table Service")
                .setTicker("LRSRadiusDemo Table Service")
                .setContentText("Broadcasting...")
//                .setSmallIcon(R.drawable.ic_launcher)
//                .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .addAction(android.R.drawable.ic_media_play, "Broadcast", broadcastIntentService)
                .build()

        startForeground(Constants.NOTIFICATION_ID.FOREGROUND_SERVICE, notification)
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(LOG_TAG, "In onDestroy")
        Toast.makeText(this, "Service Destroyed!", Toast.LENGTH_SHORT).show()
        venuBroadcast?.stop()
    }

    override fun onBind(intent: Intent?): IBinder {
        return mBinder
    }
}