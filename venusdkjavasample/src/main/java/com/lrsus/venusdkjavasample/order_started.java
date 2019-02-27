package com.lrsus.venusdkjavasample;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.lrsus.venusdk.MobileId;
import com.lrsus.venusdk.VENUBroadcast;
import com.lrsus.venusdk.VENUCallback;
import com.lrsus.venusdk.VENUManager;
import com.lrsus.venusdk.VENUMessagingService;
import com.lrsus.venusdk.VENUServiceNumber;

import java.util.UUID;

public class order_started extends AppCompatActivity {

    VENUManager venuManager = null;
    OrderBroadcastReceiver receiver = new OrderBroadcastReceiver();
    TextView locationTextView = null;

    private class OrderBroadcastReceiver extends VENUCallback {

        @Override
        public void onServiceNumber(VENUServiceNumber serviceNumber) {

        }

        @Override
        public void onServiceRequested(VENUServiceNumber serviceNumber) {

        }

        @Override
        public void onServiceExpiration(VENUServiceNumber serviceNumber) {

        }

        @Override
        public void onServiceAccepted(VENUServiceNumber serviceNumber) {

        }

        @Override
        public void onServiceLocated(VENUServiceNumber serviceNumber) {
            String location = serviceNumber.getLocation();
            TextView locationTextView = findViewById(R.id.locatedAtTextView);
            locationTextView.setText(String.format("Located at %s", location));
        }

        @Override
        public void onServiceOrderStarted(VENUServiceNumber serviceNumber) {
        }

        @Override
        public void onServiceCleared(VENUServiceNumber serviceNumber) {
            VENUBroadcast.stopService(getApplicationContext());
            order_started.this.finish();
        }

        @Override
        public void onBroadcast() {

        }

        @Override
        public void onBroadcastFailed(int errorCode) {

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
        public VENUManager venuService() {
            return venuManager;
        }

        @Override
        public void onNoServiceNumber(UUID brandId, Object siteId, UUID mobileId) {

        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_started);
        locationTextView = findViewById(R.id.locatedAtTextView);

        venuManager = VENUManager.getInstance(
                this,
                getString(R.string.APP_ID).replace("\n", ""),
                getString(R.string.APP_SECRET).replace("\n", ""),
                MobileId.get(this)
        );
    }

    @Override
    protected void onResume() {
        super.onResume();
        VENUMessagingService.register(this, receiver);

        Intent options = getIntent();
        if (options != null && options.hasExtra("currentLocation")) {
            locationTextView.setText(String.format("Located at %s", options.getStringExtra("currentLocation")));
        }
    }

    @Override
    protected void onPause() {
        VENUMessagingService.unregister(this, receiver);

        super.onPause();
    }
}
