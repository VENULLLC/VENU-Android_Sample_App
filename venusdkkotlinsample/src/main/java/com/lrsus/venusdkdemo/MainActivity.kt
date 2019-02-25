/**
 * MainActivity.kt
 * Copyright 2018 Long Range Systems, LLC
 */
package com.lrsus.venusdkdemo

import android.Manifest
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.bluetooth.le.AdvertiseCallback
import android.content.*
import android.content.pm.PackageManager
import android.support.v4.app.ActivityCompat
import android.support.v4.content.ContextCompat
import android.support.v4.content.LocalBroadcastManager
import android.widget.*
import com.google.firebase.iid.FirebaseInstanceId
import com.lrsus.venusdk.*
import java.util.*


class MainActivity : AppCompatActivity() {
    // Callback for services
    private lateinit var mVENUSDKCallback : VENUCallback
    private val TAG = "VENUKotlinDemo"

    // UI elements
    private lateinit var requestServiceButton : Button
    private lateinit var startOrderButton: Button
    private lateinit var rangeView : TextView
    private lateinit var venuManager : VENUManager
    private lateinit var mobileId : UUID
    private var currentLocationId : Int = 0
    private var deviceToken : String? = null

    private fun brandId(): UUID {
        return UUID.fromString(getString(R.string.BRAND_ID))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        mVENUSDKCallback = VENUCallbackImplementation()
        val uuid = MobileId.get(applicationContext)
        if (uuid != null) {
            mobileId = uuid
        }

        requestServiceButton = findViewById<Button>(R.id.requestServiceButton)
        startOrderButton = findViewById<Button>(R.id.startOrderButton)
        rangeView = findViewById(R.id.beaconDistance)
        venuManager = VENUManager.getInstance(
                this,
                getString(R.string.APP_ID).replace("\n", ""),
                getString(R.string.APP_SECRET).replace("\n", ""),
                mobileId
        )
    }


    override fun onStart() {
        super.onStart()

        // Mark application as running so we avoid background notifications.
        MainApplication.setActivityRunStatus(true)

        // Check permissions
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION), 23523)
        }

        FirebaseInstanceId.getInstance().instanceId
                .addOnCompleteListener { task ->
                    if (!task.isSuccessful) {
                        Log.w(TAG, "Unable to retrieve FCM token ${task.exception}")
                    } else {
                        deviceToken = task.getResult()?.token
                    }
                }

        requestServiceButton.setOnClickListener {
//            if (currentLocationId > 0) {
                venuManager.serviceNumber(
                        brandId(),
                        3,
                        mVENUSDKCallback,
                        deviceToken
                )
//            } else {
//                Toast.makeText(applicationContext, "No location detected.", Toast.LENGTH_SHORT).show()
//            }
        }

        startOrderButton.setOnClickListener {
            venuManager.getServiceNumber()?.startOrder();
        }

        // Check for existing service number
        venuManager.checkForServiceNumber(brandId(), 3, mVENUSDKCallback)

        VENUMessagingService.register(this, mVENUSDKCallback)
        VENURange.startService(this, brandId())
        VENUMessagingService.startService(this)
    }

    override fun onStop() {
        super.onStop()

        MainApplication.setActivityRunStatus(false)
        VENURange.stopService(this)
        VENUMessagingService.stopService(this)
    }

    override fun onDestroy() {
        VENUMessagingService.unregister(this, mVENUSDKCallback)
        super.onDestroy()
    }

    /**
     * The callback provides location and status updates.
     */
    inner class VENUCallbackImplementation : VENUCallback() {
        override fun onServiceNumber(serviceNumber: VENUServiceNumber) {
            val serviceState = serviceNumber.serviceState
            val orderState = serviceNumber.orderState
            runOnUiThread {
                requestServiceButton.isEnabled = false
                requestServiceButton.text = if (serviceState == "pending") "waiting" else serviceNumber.number.toString()
                startOrderButton.isEnabled = orderState == "pending" && serviceState != "pending"
                startOrderButton.text = if (orderState == "pending") "Start Order" else "Order Started"
            }

            VENUBroadcast.startService(applicationContext, serviceNumber.macAddress())
        }

        override fun onNoServiceNumber(brandId: UUID, siteId: Any, mobileId: UUID?) {
            Toast.makeText(applicationContext, "No existing service number.", Toast.LENGTH_SHORT).show()
        }


        override fun onServiceRequested(serviceNumber: VENUServiceNumber) {
            // Service number has been accepted by server, but not by local server
            runOnUiThread {
                requestServiceButton.isEnabled = false
                requestServiceButton.text = "Waiting..."
            }

            // Start broadcasting
            VENUBroadcast.startService(applicationContext, serviceNumber.macAddress())
        }

        override fun onServiceExpiration(serviceNumber: VENUServiceNumber) {
            Toast.makeText(applicationContext, "${serviceNumber.number} expired", Toast.LENGTH_SHORT).show()

            runOnUiThread {
                requestServiceButton.isEnabled = true
                requestServiceButton.text = "Request Service Number"
            }

            // Stop broadcasting
            VENUBroadcast.stopService(applicationContext);
        }

        override fun onServiceAccepted(serviceNumber: VENUServiceNumber) {
            runOnUiThread {
                requestServiceButton.isEnabled = false
                requestServiceButton.text = serviceNumber.number.toString()
                startOrderButton.isEnabled = true
            }
        }

        override fun onServiceLocated(serviceNumber: VENUServiceNumber) {
            Toast.makeText(applicationContext, "${serviceNumber.number} located at ${serviceNumber.getLocation()}.", Toast.LENGTH_SHORT).show()
        }

        override fun onServiceOrderStarted(serviceNumber: VENUServiceNumber) {
            Toast.makeText(applicationContext, "${serviceNumber.number} started.", Toast.LENGTH_LONG).show()
            startOrderButton.isEnabled = false
            startOrderButton.text = "Order started"
        }

        override fun onServiceCleared(serviceNumber: VENUServiceNumber) {
            runOnUiThread {
                requestServiceButton.isEnabled = true
                requestServiceButton.text = "Request Service Number"
                startOrderButton.text = "Start Order";
                startOrderButton.isEnabled = false
            }

            Toast.makeText(applicationContext, "${serviceNumber.number} cleared.", Toast.LENGTH_SHORT).show()

            // Stop broadcasting
            VENUBroadcast.stopService(applicationContext);
        }

        override fun venuService(): VENUManager {
            return venuManager
        }

        /**
         * Provides location event when detected within a 5-10 meters of the locator/beacon.
         */
        override fun onRegionEntered(locationId: Int, distance: Double, initial: Boolean) {
            if (initial) {
                Toast.makeText(applicationContext, "Discovered VENU location", Toast.LENGTH_SHORT).show()
                currentLocationId = locationId

//                // Check for existing service number
//                venuManager.checkForServiceNumber(brandId(), locationId, this)
            }

            runOnUiThread {
                rangeView.text = String.format("%.2f", distance)
            }
        }

        /**
         * Provides location event when outside of location
         */
        override fun onRegionExited() {
            Toast.makeText(applicationContext, "Exited VENU location", Toast.LENGTH_SHORT).show()
            currentLocationId = 0
        }

        /**
         * Called when phone starts broadcasting
         */
        override fun onBroadcast() {
            Toast.makeText(applicationContext, "Broadcasting.", Toast.LENGTH_SHORT).show()
        }

        override fun onBroadcastStop() {
            Toast.makeText(applicationContext, "Broadcast stopped", Toast.LENGTH_SHORT).show()
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

    }

}
