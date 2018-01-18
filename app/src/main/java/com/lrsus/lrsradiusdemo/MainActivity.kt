/**
 * MainActivity.kt
 * Copyright 2018 Long Range Systems, LLC
 */
package com.lrsus.lrsradiusdemo

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.os.IBinder
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import java.util.*

class MainActivity : AppCompatActivity() {

    private var broadcastServiceBinder : BroadcastService.BroadcastServiceBinder? = null
    private var broadcastService : BroadcastService? = null
    private var mServiceRunning = false
    private var mService : Intent? = null

    // UI
    private var broadcastIdView : TextView? = null
    private var broadcastToggle : ToggleButton? = null

    private val mConnection = object : ServiceConnection {
        override fun onServiceDisconnected(name: ComponentName?) {
            mServiceRunning = false
        }

        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            broadcastServiceBinder = service as BroadcastService.BroadcastServiceBinder?
            broadcastService = broadcastServiceBinder?.getService()
            mServiceRunning = true
            broadcastIdView?.text = broadcastService?.serviceNumber.toString()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        broadcastIdView = findViewById(R.id.broadcastIdView) as TextView
        broadcastToggle = findViewById(R.id.broadcastSwitch) as ToggleButton
    }

    override fun onStart() {
        super.onStart()

        val broadcastEnableText = Toast.makeText(applicationContext, "Broadcasting enabled.", Toast.LENGTH_SHORT)
        val broadcastDisableText = Toast.makeText(applicationContext, "Broadcasting disabled.", Toast.LENGTH_SHORT)

        if (mService == null) {
            mService = Intent(this@MainActivity, BroadcastService::class.java)
        }

        broadcastToggle?.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                broadcastDisableText.cancel()
                broadcastEnableText.show()

                mService?.action = Constants.ACTION.STARTFOREGROUND_ACTION
                BroadcastService.IS_SERVICE_RUNNING = true;

                if (!mServiceRunning) {
                    startService(mService)
                    bindService(mService, mConnection, Context.BIND_AUTO_CREATE)
                } else {
                    val serviceNumber : Int? = broadcastService?.serviceNumber

                    // As service is starting, we may not get a service number.
                    if (serviceNumber != null) {
                        broadcastIdView?.text = broadcastService?.serviceNumber.toString()
                    }
                }
            } else {
                broadcastEnableText.cancel()
                broadcastDisableText.show()

                mService?.action = Constants.ACTION.STOPFOREGROUND_ACTION
                BroadcastService.IS_SERVICE_RUNNING = false;

                broadcastIdView?.text = ""
                stopService(mService)
                unbindService(mConnection)
                mServiceRunning = false
            }

        }
    }

    override fun onStop() {
        super.onStop()
        unbindService(mConnection)
        stopService(mService)
        mServiceRunning = true
    }
}
