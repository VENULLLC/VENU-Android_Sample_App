package com.lrsus.venusdkjavasample;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.lrsus.venusdk.MobileId;
import com.lrsus.venusdk.VENUManager;
import com.lrsus.venusdk.VENUMessagingService;
import com.lrsus.venusdk.VENUServiceNumber;

public class order_started extends AppCompatActivity {

    VENUManager venuManager = null;
    OrderBroadcastReceiver receiver = new OrderBroadcastReceiver();


    private class OrderBroadcastReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            VENUServiceNumber serviceNumber = VENUServiceNumber.fromIntent(venuManager, intent);
            Toast.makeText(getApplicationContext(), "Order was updated.", Toast.LENGTH_SHORT).show();
            String location = serviceNumber.getLocation();
            TextView locationTextView = findViewById(R.id.locatedAtTextView);
            locationTextView.setText(location);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_started);
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
    }

    @Override
    protected void onPause() {
        VENUMessagingService.unregister(this, receiver);

        super.onPause();
    }
}
