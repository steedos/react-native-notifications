package com.wix.reactnativenotifications;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NotificationManagerCompat;
import android.util.Log;

import com.facebook.react.bridge.ActivityEventListener;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.ReadableMap;
import com.wix.reactnativenotifications.core.AppLifecycleFacade;
import com.wix.reactnativenotifications.core.AppLifecycleFacadeHolder;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.core.ReactAppLifecycleFacade;
import com.wix.reactnativenotifications.core.notification.IPushNotification;
import com.wix.reactnativenotifications.core.notification.PushNotification;
import com.wix.reactnativenotifications.core.notification.PushNotificationProps;
import com.wix.reactnativenotifications.core.notificationdrawer.IPushNotificationsDrawer;
import com.wix.reactnativenotifications.core.notificationdrawer.PushNotificationsDrawer;
import com.wix.reactnativenotifications.gcm.FcmInstanceIdRefreshHandlerService;
import com.wix.reactnativenotifications.helpers.ApplicationBadgeHelper;

import com.google.firebase.FirebaseApp;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

import com.wix.reactnativenotifications.huawei.HuaweiInstanceIdRefreshHandlerService;
import com.wix.reactnativenotifications.gcm.FcmToken;

public class RNNotificationsModule extends ReactContextBaseJavaModule implements ActivityEventListener {

    public static ReactApplicationContext mReactAppContext;
    public static String mPushProvider;

    public RNNotificationsModule(Application application, ReactApplicationContext reactContext) {
        super(reactContext);
        if (AppLifecycleFacadeHolder.get() instanceof ReactAppLifecycleFacade) {
            ((ReactAppLifecycleFacade) AppLifecycleFacadeHolder.get()).init(reactContext);
        }

        reactContext.addActivityEventListener(this);
    }

    @Override
    public String getName() {
        return "WixRNNotifications";
    }

    @Override
    public void initialize() {
        Log.d(LOGTAG, "Native module init");
        mReactAppContext = getReactApplicationContext();
        mPushProvider = getPushProvider();

        if (mPushProvider == "huawei")
            startHuaweiIntentService(HuaweiInstanceIdRefreshHandlerService.EXTRA_IS_APP_INIT);
        else 
            startGcmIntentService(FcmInstanceIdRefreshHandlerService.EXTRA_IS_APP_INIT);

        final IPushNotificationsDrawer notificationsDrawer = PushNotificationsDrawer.get(getReactApplicationContext().getApplicationContext());
        notificationsDrawer.onAppInit();
    }

    public String getPushProvider() {
        String deviceBrand = Build.BRAND;
        String manufacturer = Build.MANUFACTURER;
        String pushProvider = "gcm";
        if(manufacturer.equals("Xiaomi")
            || deviceBrand.contains("Xiaomi")
            || deviceBrand.contains("xiaomi") ){
        } else if (manufacturer.equals("HUAWEI")
        || deviceBrand.contains("HUAWEI")
        || deviceBrand.contains("Huawei")
        || deviceBrand.contains("huawei")
        || deviceBrand.contains("HONOR")
        || deviceBrand.contains("honor")){
            pushProvider = "huawei";
        }
        return pushProvider;
    }

    @Override
    public void onActivityResult(Activity activity, int requestCode, int resultCode, Intent data) {

    }

    @Override
    public void onNewIntent(Intent intent) {
        Bundle notificationData = intent.getExtras();
        if (notificationData != null) {
            final IPushNotification notification = PushNotification.get(getReactApplicationContext().getApplicationContext(), notificationData);
            if (notification != null) {
                notification.onOpened();
            }
        }
    }

    @ReactMethod
    public void refreshToken() {
        Log.d(LOGTAG, "Native method invocation: refreshToken()");
        startGcmIntentService(FcmInstanceIdRefreshHandlerService.EXTRA_MANUAL_REFRESH);
        startHuaweiIntentService(HuaweiInstanceIdRefreshHandlerService.EXTRA_MANUAL_REFRESH);

    }

    @ReactMethod
    public void setApplicationIconBadgeNumber(int number) {
        ApplicationBadgeHelper.instance.setApplicationIconBadgeNumber(getReactApplicationContext(), number);
    }

    @ReactMethod
    public void getInitialNotification(final Promise promise) {
        Log.d(LOGTAG, "Native method invocation: getInitialNotification");
        Object result = null;

        try {
            final PushNotificationProps notification = InitialNotificationHolder.getInstance().get();
            if (notification == null) {
                return;
            }

            result = Arguments.fromBundle(notification.asBundle());
        } finally {
            promise.resolve(result);
        }
    }

    @ReactMethod
    public void postLocalNotification(ReadableMap notificationPropsMap, int notificationId) {
        Log.d(LOGTAG, "Native method invocation: postLocalNotification");
        final Bundle notificationProps = Arguments.toBundle(notificationPropsMap);
        notificationProps.putString("localNotification", "true");
        final IPushNotification pushNotification = PushNotification.get(getReactApplicationContext().getApplicationContext(), notificationProps);
        pushNotification.onPostRequest(notificationId);
    }

    @ReactMethod
    public void scheduleLocalNotification(ReadableMap notificationPropsMap, int notificationId) {
        Log.d(LOGTAG, "Native method invocation: scheduleLocalNotification");
        final Bundle notificationProps = Arguments.toBundle(notificationPropsMap);

        notificationProps.putString("localNotification", "true");

        if (notificationProps.getString("id") == null) {
            notificationProps.putString("id", String.valueOf(notificationId));
        }
        final IPushNotification pushNotification = PushNotification.get(getReactApplicationContext().getApplicationContext(), notificationProps);
        pushNotification.onScheduleRequest(notificationId);
    }

    @ReactMethod
    public void cancelLocalNotification(int notificationId) {
        IPushNotificationsDrawer notificationsDrawer = PushNotificationsDrawer.get(getReactApplicationContext().getApplicationContext());
        notificationsDrawer.onNotificationClearRequest(notificationId);
    }

    @ReactMethod
    public void isRegisteredForRemoteNotifications(Promise promise) {
        boolean hasPermission = NotificationManagerCompat.from(getReactApplicationContext()).areNotificationsEnabled();
        promise.resolve(new Boolean(hasPermission));
    }

    @ReactMethod
    public void cancelAllLocalNotifications() {
        IPushNotificationsDrawer notificationDrawer = PushNotificationsDrawer.get(getReactApplicationContext().getApplicationContext());
        notificationDrawer.onCancelAllLocalNotifications();
    }

    protected void startGcmIntentService(String extraFlag) {
        final Context appContext = getReactApplicationContext().getApplicationContext();
        final Intent tokenFetchIntent = new Intent(appContext, FcmInstanceIdRefreshHandlerService.class);
        tokenFetchIntent.putExtra(extraFlag, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(tokenFetchIntent);
        } else {
            appContext.startService(tokenFetchIntent);
        }
    }

    protected void startHuaweiIntentService(String extraFlag) {
        final Context appContext = getReactApplicationContext().getApplicationContext();
        final Intent tokenFetchIntent = new Intent(appContext, HuaweiInstanceIdRefreshHandlerService.class);
        tokenFetchIntent.putExtra(extraFlag, true);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            appContext.startForegroundService(tokenFetchIntent);
        } else {
            appContext.startService(tokenFetchIntent);
        }
    }
}
