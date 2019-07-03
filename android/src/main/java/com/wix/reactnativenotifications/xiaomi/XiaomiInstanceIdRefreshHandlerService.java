package com.wix.reactnativenotifications.xiaomi;

import android.app.IntentService;
import android.app.Notification;
import android.os.Build;
import android.content.Intent;

import com.wix.reactnativenotifications.gcm.IFcmToken;

public class XiaomiInstanceIdRefreshHandlerService extends IntentService {

    public static String EXTRA_IS_APP_INIT = "isAppInit";
    public static String EXTRA_MANUAL_REFRESH = "doManualRefresh";
    public static IFcmToken xiaomiToken = null;

    public XiaomiInstanceIdRefreshHandlerService() {
        super(XiaomiInstanceIdRefreshHandlerService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForeground(1, new Notification());
        }

    }

    @Override
    protected void onHandleIntent(Intent intent) {
        // Xiaomi Push needs CurrentActivity param to initialze, so wait 5 seconds for app startup
        try {
            Thread.sleep(5000);
        } catch (Exception e){
        }
        IFcmToken xiaomiToken = XiaomiToken.get(this);
        if (xiaomiToken == null) {
            return;
        }

        if (intent.getBooleanExtra(EXTRA_IS_APP_INIT, false)) {
            xiaomiToken.onAppReady();
        } else if (intent.getBooleanExtra(EXTRA_MANUAL_REFRESH, false)) {
            xiaomiToken.onManualRefresh();
        } else {
            xiaomiToken.onNewTokenReady();
        }
    }
}
