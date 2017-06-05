package com.wix.reactnativenotifications.core.notification;

import android.app.Application;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class PushNotificationPublisher extends BroadcastReceiver {
    final static String NOTIFICATION_ID = "notificationId";

    @Override
    public void onReceive(Context context, Intent intent) {
        int id = intent.getIntExtra(NOTIFICATION_ID, 0);
        long currentTime = System.currentTimeMillis();

        Application applicationContext = (Application) context.getApplicationContext();
        final IPushNotification pushNotification = PushNotification.get(applicationContext, intent.getExtras());

        Log.i("PN_TAG", "PushNotificationPublisher: Prepare To Publish: " + id + ", Now Time: " + currentTime);

        pushNotification.sendNotificationScheduled(id);
    }
}