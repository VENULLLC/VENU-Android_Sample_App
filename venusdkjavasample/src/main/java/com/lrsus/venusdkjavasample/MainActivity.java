package com.lrsus.venusdkjavasample;

import android.Manifest;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.IBinder;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.lrsus.venusdk.VENUBroadcast;
import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENURange;

import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private TextView distanceTextView = null;
    private VENURange myBrandRange = null;
    private VENUBroadcast venuBroadcast = null;

    private ServiceConnection myBrandRangeServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Cast to VENURangeBinder to get the service.
            myBrandRange = ((VENURange.VENURangeBinder)iBinder).getService();
            // Set up the callback
            myBrandRange.setCallback(venuCallback);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            myBrandRange = null;
        }
    };

    private ServiceConnection venuBroadcastServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            // Cast to VENURangeBinder to get the service.
            venuBroadcast = ((VENUBroadcast.VENUBroadcastBinder)iBinder).getService();

            // Intent to reopen activity if notification tapped.
            Intent appIntent = new Intent(MainActivity.this, MainActivity.class);
            PendingIntent pendingAppIntent = PendingIntent.getActivity(
                    MainActivity.this,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            Notification myForegroundNotification = new NotificationCompat.Builder(MainActivity.this, MainApplication.APP_NAMESPACE)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("VENUSDKJavaSample")
                    .setContentText("Broadcasting...")
                    // Action to stop VENUBroadcastService while in the foreground
                    .addAction(
                            android.R.drawable.ic_menu_close_clear_cancel,
                            "clear",
                            venuBroadcast.stopPendingIntent(getApplicationContext()))
                    .setContentIntent(pendingAppIntent)
                    .build();

            // Set up the callback
            venuBroadcast.setCallback(venuCallback);
            // Enable foreground notification
            venuBroadcast.setForegroundNotification(new Random().nextInt(2342), myForegroundNotification);
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            venuBroadcast = null;
        }
    };

    private VENUCallback venuCallback = new VENUCallback() {
        @Override
        public void onBroadcast(int serviceNumber) {
            Toast.makeText(
                    getApplicationContext(),
                    "Broadcasting with service #" + serviceNumber,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBroadcastFailed(int errorCode) {
            Toast.makeText(
                    getApplicationContext(),
                    "Failed to broadcast with error code " + errorCode,
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onBroadcastStop() {
            Toast.makeText(
                    getApplicationContext(),
                    "Broadcasting stopped.",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void enteredVENULocation(int serviceNumber, int locationId, final double distance, boolean initial) {
            // Check that the VENUBroadcast service is not already running and that this is the first
            // enter event.
            if (initial && venuBroadcast != null && !venuBroadcast.isBroadcasting()) {
                Toast.makeText(
                        getApplicationContext(),
                        "Entered brand location with service #" + serviceNumber,
                        Toast.LENGTH_SHORT).show();

                // Start broadcast with service number extra
                Intent broadcastIntent = new Intent(MainActivity.this, VENUBroadcast.class);
                broadcastIntent.putExtra("ServiceNumber", serviceNumber);
                startService(broadcastIntent);
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    distanceTextView.setText(String.format("%.2f", distance));
                }
            });
        }

        @Override
        public void onRegionExited() {
            // Are we broadcasting?
            if (venuBroadcast.isBroadcasting()) {
                stopService(new Intent(getApplicationContext(), VENUBroadcast.class));
            }

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    distanceTextView.setText("Discovering...");
                }
            });
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        distanceTextView = findViewById(R.id.distanceMeters);
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3832);
        }

        // Bind subclassed VENURange service (MyBrandRange)
        bindService(
                new Intent(this, MyBrandRange.class),
                myBrandRangeServiceConnection,
                Context.BIND_AUTO_CREATE);

        // Do the same for VENUBroadcast...
        bindService(
                new Intent(this, VENUBroadcast.class),
                venuBroadcastServiceConnection,
                Context.BIND_AUTO_CREATE);

        // Start VENURange service
        startService(new Intent(this, MyBrandRange.class));
    }

    @Override
    protected void onStop() {
        super.onStop();

        if (venuBroadcast != null) {
            // Commenting this out would cause broadcasting to stop when the app goes into the
            // background. If utilizing foreground notifications, leave this alone.
//            if (venuBroadcast.isBroadcasting()) {
//                stopService(new Intent(getApplicationContext(), VENUBroadcast.class));
//            }

            unbindService(venuBroadcastServiceConnection);
        }

        if (myBrandRange != null) {
            if (myBrandRange.isRunning()) {
                stopService(new Intent(getApplicationContext(), MyBrandRange.class));
            }

            unbindService(myBrandRangeServiceConnection);
        }
    }
}
