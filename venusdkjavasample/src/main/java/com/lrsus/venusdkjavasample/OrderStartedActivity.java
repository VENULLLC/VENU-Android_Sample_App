package com.lrsus.venusdkjavasample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.lrsus.venusdk.VENUBroadcast;
import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENUError;
import com.lrsus.venusdk.VENUManager;
import com.lrsus.venusdk.VENUMessagingService;
import com.lrsus.venusdk.VENUServiceNumber;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class OrderStartedActivity extends AppCompatActivity implements VENUCallback {
    TextView locationTextView = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_started);
        locationTextView = findViewById(R.id.locatedAtTextView);
    }

    @Override
    protected void onResume() {
        super.onResume();
        MainApplication.venuInstance(getApplicationContext()).addListener(this);
        MainApplication.venuInstance(getApplicationContext()).start(UUID.fromString("972c065f-571c-47c9-a2fa-696c470dbee2"));
        MainApplication.venuInstance(getApplicationContext()).serviceOrderStatus();

        Intent options = getIntent();
        if (options != null && options.hasExtra("currentLocation")) {
            locationTextView.setText(String.format("Located at %s", options.getStringExtra("currentLocation")));
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        MainApplication.venuInstance(getApplicationContext()).removeListener(this);
        MainApplication.venuInstance( getApplicationContext()).stop(getApplicationContext());
    }

    @Override
    public void onServiceStatus(@Nullable VENUServiceNumber serviceNumber) {

    }

    @Override
    public void onOrderStatus(@Nullable final VENUServiceNumber serviceNumber) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                String orderState = serviceNumber.getOrderState();
                if (orderState.equals("cleared")) {
                    OrderStartedActivity.this.finish();
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
    public void onServiceAccepted(VENUServiceNumber serviceNumber) {

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
    public void onServiceOrderStarted(VENUServiceNumber serviceNumber) {
    }

    @Override
    public void onServiceCleared(VENUServiceNumber serviceNumber) {
        OrderStartedActivity.this.finish();
    }

    @Override
    public void onBroadcast() {

    }

    @Override
    public void onBroadcastStop() {

    }

    @Override
    public void onRegionEntered(int locationId, double distance, boolean initial) {

    }

    @Override
    public void onRegionExited() {

    }

    @Override
    public void onError(VENUError code, String actionOrEvent) {

    }

}
