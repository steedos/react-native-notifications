package com.wix.reactnativenotifications.huawei;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import static com.wix.reactnativenotifications.Defs.LOGTAG;
import static com.wix.reactnativenotifications.Defs.TOKEN_RECEIVED_EVENT_NAME;
import com.wix.reactnativenotifications.gcm.IFcmToken;
import com.wix.reactnativenotifications.gcm.INotificationsGcmApplication;
import com.wix.reactnativenotifications.RNNotificationsModule;

import com.huawei.android.hms.agent.HMSAgent;
import com.huawei.android.hms.agent.push.handler.GetTokenHandler;
import com.huawei.android.hms.agent.common.handler.ConnectHandler;
import com.huawei.hms.support.api.push.TokenResult;

public class HuaweiToken implements IFcmToken {

    public static Context mAppContext;
    
    protected static String sToken;

    protected HuaweiToken(Context appContext) {
        if (!(appContext instanceof ReactApplication)) {
            throw new IllegalStateException("Application instance isn't a react-application");
        }
        mAppContext = appContext;

        // Initialize Huawei Push
        Log.d(LOGTAG, "Huawei Push initializing");
    }

    private void connect() {
        HMSAgent.init(RNNotificationsModule.mReactAppContext.getCurrentActivity());
        HMSAgent.connect(RNNotificationsModule.mReactAppContext.getCurrentActivity(), new ConnectHandler() {
            @Override
            public void onConnect(int rst) {
                Log.d(LOGTAG, "Huawei Push connect end:" + rst);
            }
        });
    }
    public static IFcmToken get(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof INotificationsGcmApplication) {
            return ((INotificationsGcmApplication) appContext).getFcmToken(context);
        }
        return new HuaweiToken(appContext);
    }

    @Override
    public void onNewTokenReady() {
        synchronized (mAppContext) {
            refreshToken();
        }
    }

    @Override
    public void onManualRefresh() {
        synchronized (mAppContext) {
            if (sToken == null) {
                Log.i(LOGTAG, "Manual token refresh => asking for new token");
                refreshToken();
            } else {
                Log.i(LOGTAG, "Manual token refresh => publishing existing token ("+sToken+")");
                sendTokenToJS();
            }
        }
    }

    @Override
    public void onAppReady() {
        synchronized (mAppContext) {
            connect();
            if (sToken == null) {
                Log.i(LOGTAG, "App initialized => huawei asking for new token");
                refreshToken();
            } else {
                // Except for first run, this should be the case.
                Log.i(LOGTAG, "App initialized => huawei publishing existing token ("+sToken+")");
                sendTokenToJS();
            }
        }
    }

    protected void refreshToken() {
        // Huawei Push GetToken
        Log.d(LOGTAG, "Huawei Push refreshToken start");
        HMSAgent.Push.getToken(new GetTokenHandler() {
            @Override
            public void onResult(int rtnCode) {
                Log.d(LOGTAG, "Huawei Push refreshToken end: " + rtnCode);
            }
        }); 
    }

    protected static void sendTokenToJS() {
        final ReactInstanceManager instanceManager = ((ReactApplication) mAppContext).getReactNativeHost().getReactInstanceManager();
        final ReactContext reactContext = instanceManager.getCurrentReactContext();

        // Note: Cannot assume react-context exists cause this is an async dispatched service.
        if (reactContext != null && reactContext.hasActiveCatalystInstance()) {
            reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class).emit(TOKEN_RECEIVED_EVENT_NAME, sToken);
        }
    }
}
