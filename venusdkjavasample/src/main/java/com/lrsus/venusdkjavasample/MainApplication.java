package com.lrsus.venusdkjavasample;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.lrsus.venusdk.VENUMonitor;

import java.util.Random;
import java.util.UUID;

/**
 * For the main application, we show a notification when the BRAND_ID is heard
 * from the phone.
 */
public class MainApplication extends VENUMonitor {

    // Replace the following as necessary per brand and configuration.
    static String APP_NAMESPACE = "com.lrsus.venusdkjavasample";
    static UUID BRAND_ID = UUID.fromString("671d3a8e-ee94-4395-9177-d5382d75ff10");

    @Override
    public void onCreate() {
        super.onCreate();

        // Initialize notification channel
        createNotificationChannel();
    }

    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = APP_NAMESPACE;
//            String description = getString(R.string.channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(APP_NAMESPACE, name, importance);

            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

    @Override
    public void venuDiscovered(Integer locationId) {
        // We'll set up a notification
        Notification notification = new NotificationCompat.Builder(this, APP_NAMESPACE)
                .setSmallIcon(R.mipmap.ic_launcher_round)
                .setContentTitle(getResources().getString(R.string.app_name))
                .setContentText("Detected a location. Open the app!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
        notificationManager.notify(new Random().nextInt(600) + 100, notification);
    }

    @Override
    public String appNamespace() {
        return APP_NAMESPACE;
    }

    @Override
    public UUID brandId() {
        return BRAND_ID;
    }
}
