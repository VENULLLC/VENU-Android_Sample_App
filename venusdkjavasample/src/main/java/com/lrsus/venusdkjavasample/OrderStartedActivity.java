package com.lrsus.venusdkjavasample;

import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENUError;
import com.lrsus.venusdk.VENUServiceNumber;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OrderStartedActivity extends AppCompatActivity implements VENUCallback {
    private TextView locationTextView = null;
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
        setContentView(R.layout.activity_order_started);
        locationTextView = findViewById(R.id.locatedAtTextView);
        serviceNumberTextView = findViewById(R.id.serviceNumberOrderText);
        deviceStatusTextView = findViewById(R.id.deviceStatusText2);
        broadcastStatusTextView = findViewById(R.id.broadcastStatusTextView2);
    }

    @Override
    protected void onStart() {
        super.onStart();
        boolean connected = MainApplication.venuInstance(getApplicationContext()).isConnected();

        deviceStatusTextView.setText(String.format("Device State: %s", connected ? "Connected" : "Disconnected"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));

        MainApplication.venuInstance(getApplicationContext()).addListener(this);
        MainApplication.venuInstance( getApplicationContext()).start(UUID.fromString(getString(R.string.LOCATION_GUID)));

        // Initialize broadcast
        MainApplication.venuInstance( getApplicationContext()).startBroadcast(getApplicationContext());

        Intent options = getIntent();
        serviceNumberTextView.setText(Integer.toString(options.getIntExtra("serviceNumber", 10)));
        if (options != null && options.hasExtra("currentLocation")) {
            locationTextView.setText(String.format("Located at %s", options.getStringExtra("currentLocation")));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(mReceiver);

        MainApplication.venuInstance(getApplicationContext()).removeListener(this);
        MainApplication.venuInstance( getApplicationContext()).stop(getApplicationContext());

        // Stop broadcasting when in background.
        MainApplication.venuInstance( getApplicationContext()).stopBroadcast(getApplicationContext());
    }

    @Override
    public void onServiceStatus(@Nullable final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String orderState = serviceNumber.getOrderState();
                if (orderState.equals("cleared") || orderState.equals("pending")) {
                    // We have a service, but no associated order, so we'll go back to previous
                    // activity...
                    OrderStartedActivity.this.finish();
                } else {
                    // Update everything else accordingly..
                    serviceNumberTextView.setText(Integer.toString(serviceNumber.getNumber()));
                    locationTextView.setText(String.format("Located at %s", serviceNumber.getLocation()));
                }
            }
        });
    }

    @Override
    public void onServiceOrderCleared(VENUServiceNumber serviceNumber) {
        OrderStartedActivity.this.finish();
    }

    @Override
    public void onServiceExpiration(VENUServiceNumber serviceNumber) {
        onServiceCleared(serviceNumber);
    }

    @Override
    public void onServiceAccepted(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serviceNumberTextView.setText(Integer.toString(serviceNumber.getNumber()));
            }
        });
    }

    @Override
    public void onServiceLocated(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String location = serviceNumber.getLocation();
                TextView locationTextView = findViewById(R.id.locatedAtTextView);
                locationTextView.setText(String.format("Located at %s", location));
            }
        });
    }

    @Override
    public void onServiceOrderStarted(final VENUServiceNumber serviceNumber) {
        // Since we have an order started, we'll just update the activity
        // just in case.
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serviceNumberTextView.setText(Integer.toString(serviceNumber.getNumber()));
            }
        });
    }

    @Override
    public void onServiceCleared(VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                serviceNumberTextView.setText("...");
            }
        });
        OrderStartedActivity.this.finish();
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

    }

    @Override
    public void onRegionExited() {

    }

    @Override
    public void onError(final VENUError code, final String actionOrEvent) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                switch (code) {
                    // If an action is performed such as startServiceOrder() or clearService() without an active service,
                    // VENU controller will notify of no such service. Here, related service expiration logic can be performed.
                    case SERVICE_DOES_NOT_EXIST:
                        // No service means we don't necessarily have visibility into an order, so we'll just finish this activity.
                        OrderStartedActivity.this.finish();
                        break;
                    case INVALID_SERVICE_EVENT:
                        break;
                    // These codes can indicate that the SDK received an event that it does not know of. This can happen if there's a new
                    // service event that is being sent by the controller.
                    case UNKNOWN_EVENT:
                        break;
                    case INVALID_MESSAGE:
                        break;
                    case INVALID_MESSAGE_TYPE:
                        break;
                    case UNEXPECTED_ERROR:
                        break;
                    // An action was performed but no acknowledgement was received within time.
                    case ACTION_TIMEOUT:
                        deviceStatusTextView.setText("Device Status: Timed out. Did not hear " + actionOrEvent + " response.");
                        break;
                    case BLUETOOTH_NOT_ENABLED:
                        // It's recommended to check bluetooth state like in shown in the receiver of MainActivity.
                        Toast.makeText(getApplicationContext(), "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
                        break;
                    case CONNECTION_ERROR:
                        // Communication with controller was cut off.
                        deviceStatusTextView.setText("There was a problem talking to site.");
                        break;
                    case DEVICE_DISCONNECTED:
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
