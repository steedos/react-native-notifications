package com.wix.reactnativenotifications.huawei;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import android.content.Intent;

import com.wix.reactnativenotifications.gcm.IFcmToken;

public class HuaweiInstanceIdRefreshHandlerService extends IntentService {

    public static String EXTRA_IS_APP_INIT = "isAppInit";
    public static String EXTRA_MANUAL_REFRESH = "doManualRefresh";
    public static IFcmToken huaweiToken = null;

    public HuaweiInstanceIdRefreshHandlerService() {
        super(HuaweiInstanceIdRefreshHandlerService.class.getSimpleName());
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
        // Huawei Push needs CurrentActivity param to initialze, so wait 5 seconds for app startup
        try {
            Thread.sleep(5000);
        } catch (Exception e){
        }
        
        IFcmToken huaweiToken = HuaweiToken.get(this);
        if (huaweiToken == null) {
            return;
        }

        if (intent.getBooleanExtra(EXTRA_IS_APP_INIT, false)) {
            huaweiToken.onAppReady();
        } else if (intent.getBooleanExtra(EXTRA_MANUAL_REFRESH, false)) {
            huaweiToken.onManualRefresh();
        } else {
            huaweiToken.onNewTokenReady();
        }
    }
}
