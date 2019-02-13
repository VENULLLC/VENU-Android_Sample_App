/**
 * MainActivity.kt
 * Copyright 2018 Long Range Systems, LLC
 */
package com.lrsus.venusdkdemo

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.util.Log
import android.widget.CompoundButton
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import android.app.PendingIntent
import android.bluetooth.le.AdvertiseCallback
import android.os.Build
import android.support.v4.app.NotificationCompat
import com.android.volley.Request
import com.lrsus.venusdk.*
import java.util.*


class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {

    // Callback for services
    private val mVENUSDKCallback : VENUCallback = VENUCallbackImplementation()

    // Service binders
    private var broadcastServiceBinder : VENUBroadcast.VENUBroadcastBinder? = null
    private var rangeServiceServiceBinder : VENURange.VENURangeBinder? = null

    // Library services
    private var broadcastService : VENUBroadcast? = null
    private var rangeService : VENURange? = null

    // UI elements
    private var broadcastIdView : TextView? = null
    private var broadcastToggle : ToggleButton? = null
    private lateinit var rangeView : TextView
    private lateinit var service : VENUService


    /**
     * Set up service connection so we can set up the callbacks for VENURange
     */
    private val mRangeConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(MainApplication.TAG, "Broadcasting service connected")

            rangeServiceServiceBinder = service  as VENURange.VENURangeBinder
            rangeService = rangeServiceServiceBinder?.getService()

            // Prevent ranging if we're already broadcasting (In the case of manual broadcasting)
            if (broadcastService == null || broadcastService?.isBroadcasting() == false) {

                // Set up callbacks
                rangeService?.setCallback(mVENUSDKCallback)

                // Start the service if it's not already running
                if (!rangeService!!.isRunning()) {
                    val rangeIntent = Intent(this@MainActivity, RangeService::class.java)
                    startService(rangeIntent)
                }
            }
        }

        override fun onServiceDisconnected(p0: ComponentName?) {
            Log.d(MainApplication.TAG, "Broadcasting service disconnected")
        }

    }

    /**
     * Set up service connection so we can set up the callbacks for VENUBroadcast
     * and to enable the foreground notification
     */
    private val mBroadcastConnection = object : ServiceConnection {

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d(MainApplication.TAG, "Broadcast service disconnected")
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d(MainApplication.TAG, "Broadcast service connected")

            broadcastServiceBinder = service as VENUBroadcast.VENUBroadcastBinder?
            broadcastService = broadcastServiceBinder?.getService()

            // Build notification for foreground service
            val builder = NotificationCompat.Builder(applicationContext, MainApplication.APP_NAMESPACE)
            builder.setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("Broadcasting your location.")
                    .setOngoing(true)
                    // Add clear action to stop the broadcasting service
                    .addAction(
                            android.R.drawable.ic_notification_clear_all,
                            "Clear",
                            broadcastService?.stopPendingIntent(applicationContext)
                    )

            // Reopen this activity when the foreground notification is tapped
            val intent = Intent(this@MainActivity, MainActivity::class.java)
            val pendingIntent = PendingIntent.getActivity(
                    this@MainActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
            )
            builder.setContentIntent(pendingIntent)

            // Enable foreground notification
            broadcastService?.setForegroundNotification(62362, builder.build())

            // Connect the callback handlers.
            broadcastService?.setCallback(mVENUSDKCallback)

            // Check for an already running broadcast in the case we come back to the activity
            // while the broadcasting is in the forground
            if (broadcastService?.isBroadcasting() == true) {
                // Update UI state accordingly
                broadcastIdView?.text = broadcastService?.serviceNumber().toString()
                broadcastToggle?.isChecked = true
            }
        }
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
            val channel = NotificationChannel(MainApplication.APP_NAMESPACE, name, importance).apply {
                description = descriptionText
            }
            // Register the channel with the system
            val notificationManager: NotificationManager =
                    getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        broadcastIdView = findViewById(R.id.broadcastIdView) as TextView
        broadcastToggle = findViewById(R.id.broadcastSwitch) as ToggleButton
        rangeView = findViewById(R.id.beaconDistance)
        service = VENUService.getInstance(
                this,
                getString(R.string.APP_ID).replace("\n", ""),
                getString(R.string.APP_SECRET).replace("\n", "")
        )
        createNotificationChannel()
    }


    override fun onStart() {
        super.onStart()

        // Mark application as running so we avoid background notifications.
        MainApplication.setActivityRunStatus(true)

        // Bind VENUBroadcast
        bindService(
                Intent(this@MainActivity, VENUBroadcast::class.java),
                mBroadcastConnection,
                Context.BIND_AUTO_CREATE)

        // Bind RangeService which extends VENURange
        bindService(
                Intent(this@MainActivity, RangeService::class.java),
                mRangeConnection,
                Context.BIND_AUTO_CREATE)

        broadcastToggle?.setOnCheckedChangeListener(this)
    }

    override fun onStop() {
        super.onStop()

        if (broadcastService == null || !broadcastService!!.isBroadcasting()) {
            // Mark activity as not running to restart notifications but only if
            // we're no longer broadcasting.
            MainApplication.setActivityRunStatus(false)
        }

        if (rangeService!!.isRunning()) {
            stopService(Intent(this@MainActivity, RangeService::class.java))
        }

        unbindService(mBroadcastConnection)
        unbindService(mRangeConnection)
    }

    override fun onCheckedChanged(button : CompoundButton?, isChecked : Boolean) {
        if (isChecked) {
            if (broadcastService != null) {
                // Manual broadcasting. Generate a random service number
                val manualServiceNumber = 100 + Random().nextInt(154)

                val broadcastIntent = Intent(this@MainActivity, VENUBroadcast::class.java)
                broadcastIntent.putExtra("ServiceNumber", manualServiceNumber)

                startService(broadcastIntent)
            } else {
                Toast.makeText(applicationContext, "Not Ready", Toast.LENGTH_SHORT)
                button?.isChecked = false
            }

        } else {
            if (broadcastService!!.isBroadcasting()) {
                broadcastService?.stop()
            }

            broadcastIdView?.text = ""
        }
    }


    /**
     * The callback provides location and status updates.
     */
    inner class VENUCallbackImplementation : VENUCallback() {

        /**
         * Provides location event when detected within a 5-10 meters of the locator/beacon.
         */
        override fun enteredVENULocation(serviceNumber: Int, locationId : Int, distance : Double, initial : Boolean) {
            Toast.makeText(applicationContext, "Discovered VENU location", Toast.LENGTH_SHORT).show()

            runOnUiThread {
                rangeView.text = String.format("%.2f", distance)
            }

            if (initial && !broadcastService!!.isBroadcasting()) {
                // Create an intent with service number generated to VENUBroadcast service
                val broadcastIntent = Intent(this@MainActivity, VENUBroadcast::class.java)
                broadcastIntent.putExtra("ServiceNumber", serviceNumber)
                startService(broadcastIntent)
            }
        }

        /**
         * Provides location event when outside of location
         */
        override fun exitedVENULocation() {
            Toast.makeText(applicationContext, "Exited VENU location", Toast.LENGTH_SHORT).show()
            if (broadcastService!!.isBroadcasting() == true) {
                broadcastService?.stop()
            }
        }

        /**
         * Called when phone starts broadcasting
         */
        override fun onBroadcast(serviceNumber: Int) {
            Log.d(MainApplication.TAG, "Broadcasting with service number " + serviceNumber)
//            Toast.makeText(applicationContext, "Broadcast succeeded", Toast.LENGTH_SHORT).show()


            runOnUiThread {
                broadcastIdView?.text = serviceNumber.toString()
                broadcastToggle?.isChecked = true
            }
        }

        override fun onBroadcastFailed(errorCode : Int) {
            // Handle system error code
            if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED) {
                Toast.makeText(applicationContext, "Broadcast already started.", Toast.LENGTH_SHORT).show()
            }
            else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR) {
                Toast.makeText(applicationContext, "Unable to broadcast due to internal error. Check logs.", Toast.LENGTH_SHORT).show()
            }
            else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
                Toast.makeText(applicationContext, "This phone cannot support BLE advertising.", Toast.LENGTH_SHORT).show()
            }
            else if (errorCode == AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
                Toast.makeText(applicationContext, "Too many advertisers.", Toast.LENGTH_SHORT).show()
            }
        }

        override fun onBroadcastStop() {
            Toast.makeText(applicationContext, "Broadcast stopped", Toast.LENGTH_SHORT).show()


            runOnUiThread {
                broadcastIdView?.text = ""
                broadcastToggle?.isChecked = false
            }
        }

    }

}
