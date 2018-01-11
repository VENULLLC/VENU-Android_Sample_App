package com.lrsus.lrsradiusdemo

import android.content.Intent
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import android.widget.ToggleButton
import org.altbeacon.beacon.BeaconTransmitter
import org.altbeacon.beacon.BeaconParser
import org.altbeacon.beacon.Beacon
import java.util.*

class MainActivity : AppCompatActivity() {

    private var mBroadcastId: Int = 0
    private val mOrgUUID: String = "96B68E44-D14D-428B-9237-082B6C04623F"
    private var mAltBeaconTransmitter : BeaconTransmitter? = null
    private var mBeacon : Beacon? = null
    private var mBeaconParser : BeaconParser? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val random = Random()
        val broadcastIdView = findViewById(R.id.broadcastIdView) as TextView
        val broadcastToggle = findViewById(R.id.broadcastSwitch) as ToggleButton

        // Generate beacon ID
        if (BroadcastService.mBroadcastId == 0) {
            BroadcastService.mBroadcastId = random.nextInt(65535) + 1
            broadcastIdView.text = BroadcastService.mBroadcastId.toString()
        }

        val broadcastEnableText = Toast.makeText(applicationContext, "Broadcasting enabled.", Toast.LENGTH_SHORT)
        val broadcastDisableText = Toast.makeText(applicationContext, "Broadcasting disabled.", Toast.LENGTH_SHORT)

        broadcastToggle.setOnCheckedChangeListener { _, isChecked ->
            val service : Intent = Intent(this@MainActivity, BroadcastService::class.java)

            if (isChecked) {
                broadcastDisableText.cancel()
                broadcastEnableText.show()
                service.action = Constants.ACTION.STARTFOREGROUND_ACTION
                BroadcastService.IS_SERVICE_RUNNING = true;
            } else {
                broadcastEnableText.cancel()
                broadcastDisableText.show()
                service.action = Constants.ACTION.STOPFOREGROUND_ACTION
                BroadcastService.IS_SERVICE_RUNNING = false;
            }

            startService(service)
        }
    }
}
