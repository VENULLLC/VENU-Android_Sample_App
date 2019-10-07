package com.lrsus.venusdkjavasample;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.Toast;

import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENUError;
import com.lrsus.venusdk.VENUManager;
import com.lrsus.venusdk.VENUServiceNumber;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class MainActivity extends AppCompatActivity implements VENUCallback {

    private Button requestServiceButton = null;
    private Button startOrderButton = null;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check location permission in order to use Bluetooth.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request permission
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, 3832);
        }

        requestServiceButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
            MainApplication.venuInstance(getApplicationContext()).startServiceNumber();
            }
        });

        startOrderButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (MainApplication.getServiceNumber() != null) {
                    MainApplication.getServiceNumber().startOrder();
                }
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(mReceiver, new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED));
        MainApplication.venuInstance(getApplicationContext()).addListener(this);
        MainApplication.venuInstance( getApplicationContext()).start(UUID.fromString("88b92b5a-211b-47b0-81b3-2db3c56e975a"));
        MainApplication.venuInstance(getApplicationContext()).serviceOrderStatus();
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.venuInstance(getApplicationContext()).removeListener(this);
        MainApplication.venuInstance( getApplicationContext()).stop(getApplicationContext());
    }

    public void onError(VENUError code, String actionOrEvent) {
        switch (code) {
            // If an action is performed such as startServiceOrder() or clearService() without an active service,
            // VENU controller will notify of no such service. Here, related service expiration logic can be performed.
            case SERVICE_DOES_NOT_EXIST:
                requestServiceButton.setEnabled(true);
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
            // Unable to connect which can indicate issue with mobile or WiFi connectivity.
            case CONNECTION_ERROR:
                Toast.makeText(getApplicationContext(), "Unable to connect. Check connection.", Toast.LENGTH_SHORT).show();
                break;
            // An action has been performed but it took too long.
            case ACTION_TIMEOUT:
                break;
            // An action was performed but no acknowledgement was received within time.
            case SERVICE_TIMEOUT:
                break;
            // It's recommended to check bluetooth state like in the above. VENUManager will only
            // produce error when it failed to broadcast. It won't notify you if bluetooth is turned on.
            case BLUETOOTH_NOT_ENABLED:
                Toast.makeText(getApplicationContext(), "Bluetooth not enabled.", Toast.LENGTH_SHORT).show();
                break;
            // Communication with controller was cut off.
            case DEVICE_DISCONNECTED:
                Toast.makeText(getApplicationContext(), "Disconnected.", Toast.LENGTH_SHORT).show();
                break;
            // You probably didn't call VENUManager.start(getApplicationContext(), UUID.fromString("[ENTER_YOUR_LOCATION_GUID_HERE]")) before performing an action.
            case NO_SITE_SET:
                break;
        }
    }

    public void onBroadcast() {
        Toast.makeText( getApplicationContext(), "Broadcasting", Toast.LENGTH_SHORT).show();
    }

    public void onServiceCleared(final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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

    public void onServiceLocated(VENUServiceNumber serviceNumber) {
        Toast.makeText(getApplicationContext(), "Got location " + serviceNumber.getLocation(), Toast.LENGTH_SHORT).show();
    }

    public void onServiceAccepted(VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
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
                requestServiceButton.setEnabled(false);
            }
        });
    }

    @Override
    public void onOrderStatus(@Nullable final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String orderState = serviceNumber.getOrderState();
                if (orderState.equals("started") || orderState.equals("located")) {
                    startOrderButton.setEnabled(false);
                    Intent intent = new Intent(MainActivity.this, OrderStartedActivity.class);
                    intent.putExtra("currentLocation", serviceNumber.getLocation());
                    MainActivity.this.startActivity(intent);
                }
            }
        });
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

}
