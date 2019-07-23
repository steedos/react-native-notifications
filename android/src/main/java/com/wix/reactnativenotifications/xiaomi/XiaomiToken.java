package com.wix.reactnativenotifications.xiaomi;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;
import android.app.ActivityManager;

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

import com.xiaomi.mipush.sdk.MiPushClient;
import com.wix.reactnativenotifications.gcm.FcmToken;

public class XiaomiToken implements IFcmToken {

    protected static Context mAppContext;
    protected static ReactApplicationContext mReactAppContext;
    private String mAppId;
    private String mAppKey;
    
    protected static String sToken;

    protected XiaomiToken(Context appContext) {
        if (!(appContext instanceof ReactApplication)) {
            throw new IllegalStateException("Application instance isn't a react-application");
        }
        mAppContext = appContext;
        mReactAppContext = RNNotificationsModule.mReactAppContext;

        // Initialize Xiaomi Push
        Log.d(LOGTAG, "Xiaomi Push initializing");

        //读取小米对应的appId和appSecret
        try {
            Bundle metaData = mReactAppContext.getPackageManager().getApplicationInfo(mReactAppContext.getPackageName(), PackageManager.GET_META_DATA).metaData;
            mAppId = metaData.getString("MI_PUSH_APP_ID").trim();
            mAppKey = metaData.getString("MI_PUSH_APP_KEY").trim();
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(LOGTAG, "can't find MI_PUSH_APP_ID or MI_PUSH_APP_KEY in AndroidManifest.xml");
        } catch (NullPointerException e) {
            Log.e(LOGTAG, "can't find MI_PUSH_APP_ID or MI_PUSH_APP_KEY in AndroidManifest.xml");
        }
    }

    public static IFcmToken get(Context context) {
        Context appContext = context.getApplicationContext();
        if (appContext instanceof INotificationsGcmApplication) {
            return ((INotificationsGcmApplication) appContext).getFcmToken(context);
        }
        return new XiaomiToken(appContext);
    }

    private boolean shouldInit() {
        ActivityManager am = ((ActivityManager) mReactAppContext.getSystemService(Context.ACTIVITY_SERVICE));
        List<ActivityManager.RunningAppProcessInfo> processInfos = am.getRunningAppProcesses();
        String mainProcessName = mReactAppContext.getPackageName();
        int myPid = android.os.Process.myPid();
        for (ActivityManager.RunningAppProcessInfo info : processInfos) {
            if (info.pid == myPid && mainProcessName.equals(info.processName)) {
                return true;
            }
        }
        return false;
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
            if (sToken == null) {
                Log.i(LOGTAG, "App initialized => Xiaomi asking for new token");
                refreshToken();
            } else {
                // Except for first run, this should be the case.
                Log.i(LOGTAG, "App initialized => Xiaomi publishing existing token ("+sToken+")");
                sendTokenToJS();
            }
        }
    }

    protected void refreshToken() {
        // Xiaomi Push GetToken
        Log.d(LOGTAG, "Xiaomi Push refreshToken start");
        //if(shouldInit()) {
        MiPushClient.registerPush(mReactAppContext.getApplicationContext(), mAppId, mAppKey);
        //}
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
