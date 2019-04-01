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
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;
import com.lrsus.venusdk.ForegroundNotification;
import com.lrsus.venusdk.MobileId;
import com.lrsus.venusdk.VENUBroadcast;
import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENUManager;
import com.lrsus.venusdk.VENUMessagingService;
import com.lrsus.venusdk.VENURange;
import com.lrsus.venusdk.VENUServiceListener;
import com.lrsus.venusdk.VENUServiceNumber;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements VENUServiceListener{

    private TextView distanceTextView = null;
    private Button requestServiceButton = null;
    private Button startOrderButton = null;
//    private VENURange myBrandRange = null;
    private VENUBroadcast venuBroadcast = null;
    private VENUManager venuManager = null;
    private String deviceToken = null;

    private ServiceConnection venuBroadcastServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            VENUBroadcast.VENUBroadcastBinder venuBinder = (VENUBroadcast.VENUBroadcastBinder)iBinder;
            venuBroadcast = venuBinder.getService();

            // Intent to reopen activity if notification tapped.
            Intent appIntent = new Intent(MainActivity.this, MainActivity.class);
            PendingIntent pendingAppIntent = PendingIntent.getActivity(
                    MainActivity.this,
                    0,
                    appIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT);

            final Notification myForegroundNotification = new NotificationCompat.Builder(MainActivity.this, MainApplication.APP_NAMESPACE)
                    .setSmallIcon(R.mipmap.ic_launcher)
                    .setContentTitle("VENUSDKJavaSample")
                    .setContentText("Broadcasting...")
                    // Action to stop VENUBroadcastService while in the foreground
                    .addAction(
                            android.R.drawable.ic_menu_close_clear_cancel,
                            "clear",
                            VENUBroadcast.stopPendingIntent(getApplicationContext()))
                    .setContentIntent(pendingAppIntent)
                    .build();

            // Enable foreground notification
            venuBinder.setForegroundNotification(new ForegroundNotification() {
                @Override
                public Notification foregroundNotification() {
                    return myForegroundNotification;
                }

                @Override
                public int foregroundNotificationId() {
                    return 8385;
                }
            });
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            venuBroadcast = null;
        }
    };

    private VENUCallback venuCallback = new VENUCallback() {

        @Override
        public VENUManager venuService() {
            return venuManager;
        }

        @Override
        public void onBroadcast() {
            Toast.makeText(
                    getApplicationContext(),
                    "Broadcasting",
                    Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onNoServiceNumber(UUID brandId, Object siteId, UUID mobileId) {
            requestServiceButton.setEnabled(true);
        }

        @Override
        public void onServiceCleared(VENUServiceNumber serviceNumber) {
            requestServiceButton.setEnabled(true);
            startOrderButton.setEnabled(false);
            VENUBroadcast.stopService(getApplicationContext());
        }

        @Override
        public void onServiceOrderStarted(VENUServiceNumber serviceNumber) {
            startOrderButton.setEnabled(false);
            Intent intent = new Intent(MainActivity.this, order_started.class);
            intent.putExtra("currentLocation", serviceNumber.getLocation());
            MainActivity.this.startActivity(intent);
        }

        @Override
        public void onServiceLocated(VENUServiceNumber serviceNumber) {
            Toast.makeText(getApplicationContext(), "Got location " + serviceNumber.getLocation(), Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceAccepted(VENUServiceNumber serviceNumber) {
            startOrderButton.setEnabled(true);
        }

        @Override
        public void onServiceExpiration(VENUServiceNumber serviceNumber) {

        }

        @Override
        public void onServiceRequested(VENUServiceNumber serviceNumber) {
            requestServiceButton.setEnabled(false);
            VENUBroadcast.startService(getApplicationContext(), serviceNumber.macAddress());
        }

        @Override
        public void onServiceNumber(VENUServiceNumber serviceNumber) {
            requestServiceButton.setEnabled(false);
            if (serviceNumber.getOrderState().equals("pending")) {
                startOrderButton.setEnabled(true);
            } else {
                Intent intent = new Intent(MainActivity.this, order_started.class);
                intent.putExtra("currentLocation", serviceNumber.getLocation());
                MainActivity.this.startActivity(intent);
            }

            VENUBroadcast.startService(getApplicationContext(), serviceNumber.macAddress());
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
        public void onRegionEntered(int locationId, double distance, boolean initial) {
            Toast.makeText(getApplicationContext(), "Entered location.", Toast.LENGTH_SHORT).show();
        }


        @Override
        public void onRegionExited() {
            Toast.makeText(getApplicationContext(), "Exiting location.", Toast.LENGTH_SHORT).show();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        requestServiceButton = findViewById(R.id.requestServiceButton);
        startOrderButton = findViewById(R.id.startOrderButton);
        venuManager = VENUManager.getInstance(
                this,
                getString(R.string.APP_ID).replace("\n", ""),
                getString(R.string.APP_SECRET).replace("\n", ""),
                MobileId.get(this)
        );
    }

    @Override
    protected void onStart() {
        super.onStart();

        // Check location permission
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3832);
        }

        // Do the same for VENUBroadcast...
        bindService(
                new Intent(this, VENUBroadcast.class),
                venuBroadcastServiceConnection,
                Context.BIND_AUTO_CREATE);

        FirebaseInstanceId.getInstance().getInstanceId().addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                if (task.isSuccessful()) {
                    deviceToken = task.getResult().getToken();
                }
            }
        });

        requestServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (deviceToken == null) {
                    Toast.makeText(getApplicationContext(), "No device token provided.", Toast.LENGTH_SHORT);
                }

                venuManager.serviceNumber(
                        MainApplication.BRAND_ID,
                        3,
                        venuCallback,
                        deviceToken
                );
            }
        });

        startOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (venuManager.getServiceNumber() != null) {
                    venuManager.getServiceNumber().startOrder();
                }
            }
        });

        venuManager.checkForServiceNumber(MainApplication.BRAND_ID, 3, venuCallback);

        // Start VENURange service
//        VENURange.startService(this, MainApplication.BRAND_ID, 5.0);
        VENURange.startService(this, MainApplication.BRAND_ID);
        VENUMessagingService.startService(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        VENUMessagingService.register(this, venuCallback);
    }

    @Override
    protected void onPause() {
        VENUMessagingService.unregister(this, venuCallback);

        super.onPause();
    }

    @Override
    protected void onStop() {
        super.onStop();
        unbindService(venuBroadcastServiceConnection);
        VENUMessagingService.stopService(this);
        VENURange.stopService(this);
    }

    @Override
    public void onServiceChanged() {
        // Open new activity
        Intent intent = new Intent(MainActivity.this, order_started.class);
        MainActivity.this.startActivity(intent);
    }
}
