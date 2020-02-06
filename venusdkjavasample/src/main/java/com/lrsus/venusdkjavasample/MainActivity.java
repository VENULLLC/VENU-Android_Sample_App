package com.lrsus.venusdkjavasample;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENUError;
import com.lrsus.venusdk.VENUServiceNumber;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements VENUCallback {

    private Button requestServiceButton = null;
    private Button startOrderButton = null;
    private TextView serviceNumberTextView = null;
    private TextView deviceStatusTextView = null;
    private TextView broadcastStatusTextView = null;


    /* Here is where you'd restart broadcasting from VENUManager as well as remind users that their
     * bluetooth is off.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive (Context context, Intent intent) {
            String action = intent.getAction();
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action)) {
                if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_OFF)
                {
                    // Bluetooth is disconnected
                    MainApplication.venuInstance(getApplicationContext()).stopBroadcast(getApplicationContext());

                    // Let user know. (Probably another way besides using Toast)
                    Toast.makeText(getApplicationContext(), "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
                } else if(intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, -1)
                        == BluetoothAdapter.STATE_ON)
                {
                    // Bluetooth is connected. VENUManager will trigger onBroadcast when the device is in tracking mode.
                    MainApplication.venuInstance(getApplicationContext()).startBroadcast(getApplicationContext());
//                    deviceStatusTextView.setText("Device Status: Bluetooth enabled");
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_main);
        requestServiceButton = findViewById(R.id.requestServiceButton);
        startOrderButton = findViewById(R.id.startOrderButton);
        serviceNumberTextView = findViewById(R.id.serviceNumberText);
        deviceStatusTextView = findViewById(R.id.deviceStatusText);
        broadcastStatusTextView = findViewById(R.id.broadcastStatusTextView);
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Location permission is only needed when listening for bluetooth such as utilizing VENUMonitor.
        // Broadcasting does NOT require location permissions.
//        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Request permission
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3832);
//        }

        requestServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                MainApplication.venuInstance(getApplicationContext()).startServiceNumber();
            }
        });

        startOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            MainApplication.venuInstance(getApplicationContext()).startServiceOrder();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        serviceNumberTextView.setText("...");

        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        // Set up listeners to receive callbacks from VENUManager
        MainApplication.venuInstance(getApplicationContext()).addListener(this);

        // Initialize VENUManager against specified location
        MainApplication.venuInstance( getApplicationContext()).start(UUID.fromString(getString(R.string.LOCATION_GUID)));

        // Initialize broadcast
        MainApplication.venuInstance( getApplicationContext()).startBroadcast(getApplicationContext());

        // Get any existing service that may be active for this device. Info will be either provided
        // onServiceStatus() or onError() with SERVICE_DOES_NOT_EXIST
        MainApplication.venuInstance(getApplicationContext()).serviceStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

        // Remove listener when activity or app is not in foreground
        MainApplication.venuInstance(getApplicationContext()).removeListener(this);

        // Stop broadcasting when in background.
        MainApplication.venuInstance( getApplicationContext()).stopBroadcast(getApplicationContext());
    }

    public void onError(final VENUError code, final String actionOrEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (code) {
                    case SERVICE_DOES_NOT_EXIST:
                        // If an action is performed such as startServiceOrder() or clearService() without an active service,
                        // VENU controller will notify of no such service. Here, related service expiration logic can also be performed.
                        requestServiceButton.setEnabled(true);
                        break;
                    case INVALID_SERVICE_EVENT:
                        // These codes can indicate that the SDK received an event that it does not know of. This can happen if there's a new
                        // service event that is being sent by the controller.
                        break;
                    case UNKNOWN_EVENT:
                        // These codes can indicate that the SDK received an event that it does not know of. This can happen if there's a new
                        // service event that is being sent by the controller.
                        break;
                    case INVALID_MESSAGE:
                        // These codes can indicate that the SDK received an event that it does not know of. This can happen if there's a new
                        // service event that is being sent by the controller.
                        break;
                    case INVALID_MESSAGE_TYPE:
                        // These codes can indicate that the SDK received an event that it does not know of. This can happen if there's a new
                        // service event that is being sent by the controller.
                        break;
                    case UNEXPECTED_ERROR:
                        break;
                    case CONNECTION_ERROR:
                        deviceStatusTextView.setText("There was a problem talking to site.");
                        break;
                    // An action was performed but no acknowledgement was received within time.
                    case ACTION_TIMEOUT:
                        deviceStatusTextView.setText("Device Status: Timed out. Did not hear " + actionOrEvent + " response.");
                        break;
                    // It's recommended to check bluetooth state like in the above receiver. VENUManager will only
                    // produce error when it fails to broadcast. It won't notify you if bluetooth is turned on.
                    case BLUETOOTH_NOT_ENABLED:
                        // Bluetooth not enabled
//                        deviceStatusTextView.setText("Device Status: Bluetooth not enabled");
                        break;
                    case DEVICE_DISCONNECTED:
                        // Unable to connect which can indicate issue with mobile or WiFi connectivity.
                        deviceStatusTextView.setText("Device Status: Disconnected");
                        break;
                    case NO_SITE_SET:
                        // You probably didn't call VENUManager.start(getApplicationContext(), UUID.fromString("[ENTER_YOUR_LOCATION_GUID_HERE]")) before performing an action.
                        deviceStatusTextView.setText("No site set.");
                        break;
                }
            }
        });
    }

    public void onServiceCleared(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serviceNumberTextView.setText("...");
                requestServiceButton.setEnabled(true);
            }
        });
    }

    public void onServiceOrderStarted(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startOrderButton.setEnabled(false);
                Intent intent = new Intent(MainActivity.this, OrderStartedActivity.class);
                intent.putExtra("serviceNumber", serviceNumber.getNumber());
                intent.putExtra("currentLocation", serviceNumber.getLocation());
                MainActivity.this.startActivity(intent);
            }
        });
    }

    public void onServiceOrderCleared(VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                startOrderButton.setEnabled(true);
            }
        });
    }

    public void onServiceLocated(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(), "Got location " + serviceNumber.getLocation(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public void onServiceAccepted(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serviceNumberTextView.setText(Integer.toString(serviceNumber.getNumber()));
                requestServiceButton.setEnabled(false);
                startOrderButton.setEnabled(true);
            }
        });
    }

    public void onServiceExpiration(VENUServiceNumber serviceNumber) {
        onServiceCleared(serviceNumber);
    }

    public void onServiceStatus(@Nullable final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String orderState = serviceNumber.getOrderState();
                requestServiceButton.setEnabled(false);
                serviceNumberTextView.setText(Integer.toString(serviceNumber.getNumber()));

                if (orderState.equals("started") || orderState.equals("located")) {
                    startOrderButton.setEnabled(false);
                    Intent intent = new Intent(MainActivity.this, OrderStartedActivity.class);
                    intent.putExtra("serviceNumber", serviceNumber.getNumber());
                    intent.putExtra("currentLocation", serviceNumber.getLocation());
                    MainActivity.this.startActivity(intent);
                } else if (orderState.equals("cleared") || orderState.equals("pending")) {
                    startOrderButton.setEnabled(true);
                }
            }
        });
    }

    @Override
    public void onBroadcast() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                broadcastStatusTextView.setText("Broadcast Status: Broadcasting");
            }
        });
    }

    @Override
    public void onBroadcastStop() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                broadcastStatusTextView.setText("Broadcast Status: Not broadcasting");
            }
        });
    }

    @Override
    public void onRegionEntered(int locationId, double distance, boolean initial) {
        Toast.makeText(getApplicationContext(), "Entered location.", Toast.LENGTH_SHORT).show();
    }


    @Override
    public void onRegionExited() {
        Toast.makeText(getApplicationContext(), "Exiting location.", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDeviceConnected(final boolean reconnected) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                deviceStatusTextView.setText(String.format("Device Status: %s", reconnected ? "Reconnected" : "Connected"));
            }
        });
    }
}
