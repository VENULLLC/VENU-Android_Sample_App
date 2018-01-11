package com.lrsus.lrsradiusdemo

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import android.app.PendingIntent
import android.support.v4.app.NotificationCompat
import org.altbeacon.beacon.BeaconTransmitter
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Beacon
import java.util.*


/**
 * Created by fali on 1/11/18.
 */
class BroadcastService : Service() {

    private val LOG_TAG : String = "BroadcastService"
    private val mOrgUUID: String = "96B68E44-D14D-428B-9237-082B6C04623F"
    private var mAltBeaconTransmitter : BeaconTransmitter? = null
    private var mBeacon : Beacon? = null
    private var mBeaconParser : BeaconParser? = null

    companion object {
        var IS_SERVICE_RUNNING : Boolean = false
        var mBroadcastId: Int = 0
    }

    override fun onCreate() {
        val random = Random()

        // Generate beacon ID
        if (mBroadcastId == 0) {
            mBroadcastId = random.nextInt(65535) + 1
        }

        mBeaconParser = BeaconParser().setBeaconLayout("m:2-3=0215,i:4-19,i:20-21,i:22-23,p:24-24")

        mBeacon = Beacon.Builder()
                .setId1(mOrgUUID)
                .setId2(mBroadcastId.toString())
                .setId3("2")
                .setManufacturer(0x004c)
                .setBeaconTypeCode(0x1502)
                .setTxPower(-56)
                .build() as Beacon?

        mAltBeaconTransmitter = BeaconTransmitter(applicationContext, mBeaconParser) as BeaconTransmitter?
        mAltBeaconTransmitter?.setBeacon(mBeacon)

        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action.equals(Constants.ACTION.STARTFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Start Foreground Intent ")
            showNotification()
            Toast.makeText(this, "Service Started!", Toast.LENGTH_SHORT).show()
            mAltBeaconTransmitter?.startAdvertising()
        } else if (intent?.action.equals(Constants.ACTION.BROADCAST_ACTION)) {
            Log.i(LOG_TAG, "Clicked Broadcast");
            Toast.makeText(this, "Clicked Broadcast!", Toast.LENGTH_SHORT).show()
        } else if (intent?.action.equals(
                Constants.ACTION.STOPFOREGROUND_ACTION)) {
            Log.i(LOG_TAG, "Received Stop Foreground Intent")
            mAltBeaconTransmitter?.stopAdvertising()
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
        mAltBeaconTransmitter?.stopAdvertising()
    }

    override fun onBind(intent: Intent?): IBinder {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}