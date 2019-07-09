package com.wix.reactnativenotifications.gcm;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.Intent;

public class FcmInstanceIdRefreshHandlerService extends IntentService {

    public static String EXTRA_IS_APP_INIT = "isAppInit";
    public static String EXTRA_MANUAL_REFRESH = "doManualRefresh";

    public FcmInstanceIdRefreshHandlerService() {
        super(FcmInstanceIdRefreshHandlerService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            final String CHANNEL_ID = "channel_00";
            final String CHANNEL_NAME = "Firebase Foreground service";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_LOW);
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
            Notification.Builder notification = new Notification.Builder(this, CHANNEL_ID);
//            notification.setChannelId(CHANNEL_ID);
            startForeground(1, notification.build());
        }

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        IFcmToken gcmToken = FcmToken.get(this);
        if (gcmToken == null) {
            return;
        }

        if (intent.getBooleanExtra(EXTRA_IS_APP_INIT, false)) {
            gcmToken.onAppReady();
        } else if (intent.getBooleanExtra(EXTRA_MANUAL_REFRESH, false)) {
            gcmToken.onManualRefresh();
        } else {
            gcmToken.onNewTokenReady();
        }
    }
}
