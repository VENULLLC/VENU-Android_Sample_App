/**
 * MainActivity.kt
 * Copyright 2018 Long Range Systems, LLC
 */
package com.lrsus.venusdkdemo

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.bluetooth.le.AdvertiseCallback
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.*
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.iid.FirebaseInstanceId
import com.lrsus.venusdk.*
import java.util.*


class MainActivity : AppCompatActivity(), CompoundButton.OnCheckedChangeListener {
    // Callback for services
    private lateinit var mVENUSDKCallback : VENUCallback
    private val TAG = "VENUKotlinDemo"

    // UI elements
    private var broadcastIdView : TextView? = null
    private var broadcastToggle : ToggleButton? = null
    private lateinit var requestServiceButton : Button
    private lateinit var rangeView : TextView
    private lateinit var venuService : VENUService
    private lateinit var mobileId : UUID
    private var currentLocationId : Int = 0
    private var deviceToken : String? = null

    private fun brandId(): UUID {
        return UUID.fromString(getString(R.string.BRAND_ID))
    }

    /**
     * Set up service connection so we can set up the callbacks for VENURange
     */
//    private val mRangeConnection = object : ServiceConnection {
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            Log.d(MainApplication.TAG, "Broadcasting service connected")
//
//            rangeServiceServiceBinder = service  as VENURange.VENURangeBinder
//            rangeService = rangeServiceServiceBinder?.getService()
//
//            // Prevent ranging if we're already broadcasting (In the case of manual broadcasting)
//            if (broadcastService == null || broadcastService?.isBroadcasting() == false) {
//
//                // Set up callbacks
//                rangeService?.setCallback(mVENUSDKCallback)
//
//                // Start the service if it's not already running
//                if (!rangeService!!.isRunning()) {
//                    val rangeIntent = Intent(this@MainActivity, RangeService::class.java)
//                    startService(rangeIntent)
//                }
//            }
//        }
//
//        override fun onServiceDisconnected(p0: ComponentName?) {
//            Log.d(MainApplication.TAG, "Broadcasting service disconnected")
//        }
//
//    }

    /**
     * Set up service connection so we can set up the callbacks for VENUBroadcast
     * and to enable the foreground notification
     */
//    private val mBroadcastConnection = object : ServiceConnection {
//
//        override fun onServiceDisconnected(name: ComponentName?) {
//            Log.d(MainApplication.TAG, "Broadcast service disconnected")
//        }
//
//        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
//            Log.d(MainApplication.TAG, "Broadcast service connected")
//
////            broadcastServiceBinder = service as VENUBroadcast.VENUBroadcastBinder?
////            broadcastService = broadcastServiceBinder?.getService()
//
//            // Build notification for foreground service
//            val builder = NotificationCompat.Builder(applicationContext, MainApplication.APP_NAMESPACE)
//            builder.setSmallIcon(R.mipmap.ic_launcher)
//                    .setContentTitle("Broadcasting your location.")
//                    .setOngoing(true)
//                    // Add clear action to stop the broadcasting service
//                    .addAction(
//                            android.R.drawable.ic_notification_clear_all,
//                            "Clear",
//                            broadcastService?.stopPendingIntent(applicationContext)
//                    )
//
//            // Reopen this activity when the foreground notification is tapped
//            val intent = Intent(this@MainActivity, MainActivity::class.java)
//            val pendingIntent = PendingIntent.getActivity(
//                    this@MainActivity, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT
//            )
//            builder.setContentIntent(pendingIntent)
//
//            // Enable foreground notification
//            broadcastService?.setForegroundNotification(62362, builder.build())
//
//            // Connect the callback handlers.
//            broadcastService?.setCallback(mVENUSDKCallback)
//
//            // Check for an already running broadcast in the case we come back to the activity
//            // while the broadcasting is in the forground
//            if (broadcastService?.isBroadcasting() == true) {
//                // Update UI state accordingly
//                broadcastIdView?.text = broadcastService?.serviceNumber().toString()
//                broadcastToggle?.isChecked = true
//            }
//        }
//    }

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
        mVENUSDKCallback = VENUCallbackImplementation()
        val uuid = MobileId.get(applicationContext)
        if (uuid != null) {
            mobileId = uuid
        }

        broadcastIdView = findViewById<TextView>(R.id.broadcastIdView)
        broadcastToggle = findViewById<ToggleButton>(R.id.broadcastSwitch)
        requestServiceButton = findViewById<Button>(R.id.requestServiceButton)
        rangeView = findViewById(R.id.beaconDistance)
        venuService = VENUService.getInstance(
                this,
                getString(R.string.APP_ID).replace("\n", ""),
                getString(R.string.APP_SECRET).replace("\n", ""),
                mobileId
        )

        createNotificationChannel()
    }


    override fun onStart() {
        super.onStart()

        // Mark application as running so we avoid background notifications.
        MainApplication.setActivityRunStatus(true)

         if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
             // Request permission
             ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 23523)
         }

        // Bind VENUBroadcast
//        bindService(
//                Intent(this@MainActivity, VENUBroadcast::class.java),
//                mBroadcastConnection,
//                Context.BIND_AUTO_CREATE)
//
//        // Bind RangeService which extends VENURange
//        bindService(
//                Intent(this@MainActivity, RangeService::class.java),
//                mRangeConnection,
//                Context.BIND_AUTO_CREATE)

        broadcastToggle?.setOnCheckedChangeListener(this)

        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Unable to retrieve FCM token ${task.exception}")
                    } else {
                        deviceToken = task.getResult()?.token
                    }
                }

        requestServiceButton.setOnClickListener {
            if (currentLocationId > 0) {
                venuService.serviceNumber(
                        brandId(),
                        currentLocationId,
                        mVENUSDKCallback,
                        deviceToken
                )
            } else {
                Toast.makeText(applicationContext, "No location detected.", Toast.LENGTH_SHORT).show()
            }
        }

        LocalBroadcastManager.getInstance(this).registerReceiver(mVENUSDKCallback, IntentFilter("VENU"))

        val rangeIntent = Intent(this@MainActivity, VENURange::class.java)
        rangeIntent.putExtra("brandId", brandId().toString())
        startService(rangeIntent)

        startService(Intent(this@MainActivity, VENUMessagingService::class.java))
    }

    override fun onStop() {
        super.onStop()

        MainApplication.setActivityRunStatus(false)
//        if (broadcastService == null || !broadcastService!!.isBroadcasting()) {
//            // Mark activity as not running to restart notifications but only if
//            // we're no longer broadcasting.
//            MainApplication.setActivityRunStatus(false)
//        }

//        if (rangeService!!.isRunning()) {
//            stopService(Intent(this@MainActivity, RangeService::class.java))
//        }

//        unbindService(mBroadcastConnection)
//        unbindService(mRangeConnection)
        stopService(Intent(this@MainActivity, VENURange::class.java))
        stopService(Intent(this@MainActivity, VENUMessagingService::class.java))
    }

    override fun onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(mVENUSDKCallback)
        super.onDestroy()
    }

    override fun onCheckedChanged(button : CompoundButton?, isChecked : Boolean) {
//        if (isChecked) {
////            if (broadcastService != null) {
//                // Manual broadcasting. Generate a random service number
//                val manualServiceNumber = 100 + Random().nextInt(154)
//
//                val broadcastIntent = Intent(this@MainActivity, VENUBroadcast::class.java)
//                broadcastIntent.putExtra("ServiceNumber", manualServiceNumber)
//
//                startService(broadcastIntent)
//            } else {
//                Toast.makeText(applicationContext, "Not Ready", Toast.LENGTH_SHORT)
//                button?.isChecked = false
//            }
//
//        } else {
//            if (broadcastService!!.isBroadcasting()) {
//                broadcastService?.stop()
//            }
//
//            broadcastIdView?.text = ""
//        }
    }


    /**
     * The callback provides location and status updates.
     */
    inner class VENUCallbackImplementation : VENUCallback() {
        override fun onServiceRequested(serviceNumber: VENUServiceNumber) {
            // Service number has been accepted by server, but not by location.
            runOnUiThread {
                requestServiceButton.isEnabled = false
                requestServiceButton.text = "Waiting..."
            }
        }

        override fun onServiceExpiration(serviceNumber: VENUServiceNumber) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onServiceAccepted(serviceNumber: VENUServiceNumber) {
            runOnUiThread {
                requestServiceButton.isEnabled = false
                requestServiceButton.text = serviceNumber.number.toString()
            }
        }

        override fun onServiceStateChanged(serviceNumber: VENUServiceNumber) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onServiceLocated(serviceNumber: VENUServiceNumber) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onServiceOrderStarted(serviceNumber: VENUServiceNumber) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onServiceOrderPaged(serviceNumber: VENUServiceNumber) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun onServiceCleared(serviceNumber: VENUServiceNumber) {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        override fun venuService(): VENUService {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

        /**
         * Provides location event when detected within a 5-10 meters of the locator/beacon.
         */
        override fun onRegionEntered(locationId: Int, distance: Double, initial: Boolean) {
            if (initial) {
                Toast.makeText(applicationContext, "Discovered VENU location", Toast.LENGTH_SHORT).show()
                currentLocationId = locationId
            }

            runOnUiThread {
                rangeView.text = String.format("%.2f", distance)
            }
//            if (initial && !broadcastService!!.isBroadcasting()) {
//                // Create an intent with service number generated to VENUBroadcast service
//                val broadcastIntent = Intent(this@MainActivity, VENUBroadcast::class.java)
//                broadcastIntent.putExtra("ServiceNumber", serviceNumber)
//                startService(broadcastIntent)
//            }
        }

        /**
         * Provides location event when outside of location
         */
        override fun onRegionExited() {
            Toast.makeText(applicationContext, "Exited VENU location", Toast.LENGTH_SHORT).show()
            currentLocationId = 0
//            if (broadcastService!!.isBroadcasting() == true) {
//                broadcastService?.stop()
//            }
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
