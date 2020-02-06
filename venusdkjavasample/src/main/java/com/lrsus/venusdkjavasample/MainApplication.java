package com.lrsus.venusdkjavasample;

import android.app.Application;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;

import com.lrsus.venusdk.MobileId;
import com.lrsus.venusdk.VENUManager;
//import com.lrsus.venusdk.VENUMonitor;
import com.lrsus.venusdk.VENUMonitorHandler;
import com.lrsus.venusdk.VENUServiceNumber;

import java.util.Random;
import java.util.UUID;

/**
 * For the main application, we show a notification when the BRAND_ID is heard
 * from the phone.
 */
public class MainApplication extends Application implements VENUMonitorHandler {

    // Replace the following as necessary per brand and configuration.
    static String APP_NAMESPACE = "com.lrsus.venusdkjavasample";
    static UUID BRAND_ID = UUID.fromString("671d3a8e-ee94-4395-9177-d5382d75ff10");
    private static VENUManager venuManager = null;
    private static VENUServiceNumber serviceNumber = null;

    @Override
    public void onCreate() {
        super.onCreate();
        BRAND_ID = UUID.fromString(getString(R.string.BRAND_ID));

//        VENUMonitor.getInstance(this).setForeground(false);
//        VENUMonitor.getInstance(this).startMonitoring(BRAND_ID, this);

        // Initialize notification channel
        createNotificationChannel();
    }

    public static VENUManager venuInstance(Context context) {
        if (venuManager == null) {
            venuManager = VENUManager.getInstance(
                    context,
                    context.getString(R.string.APP_ID).replace("\n", ""),
                    context.getString(R.string.APP_SECRET).replace("\n", ""),
                    MobileId.get(context)
            );
        }

        return venuManager;
    }

    public static VENUServiceNumber getServiceNumber() {
        return serviceNumber;
    }

    public static void setServiceNumber(VENUServiceNumber newServiceNumber) {
        serviceNumber = newServiceNumber;
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
    public void onRegionEntered(UUID brandId, Integer locationNumber) {
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
    public void onRegionExited(UUID brandId, Integer locationNumber) {

    }
}
