package com.wix.reactnativenotifications.huawei;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.wix.reactnativenotifications.core.notification.IPushNotification;
import com.wix.reactnativenotifications.core.notification.PushNotification;

import java.util.Map;
import java.util.Iterator;
import java.nio.charset.Charset;

import org.json.JSONObject;

import static com.wix.reactnativenotifications.Defs.LOGTAG;
import com.wix.reactnativenotifications.gcm.IFcmToken;

import com.huawei.hms.support.api.push.PushReceiver;

public class HuaweiInstanceIdListenerService extends PushReceiver {

    @Override
    public void onToken(Context context, String token, Bundle bundle) {
        Log.d(LOGTAG, "New token from Huawei Push: " + bundle);
        super.onToken(context, token);
        if (HuaweiToken.mAppContext == null) {
            return;
        }
        HuaweiToken.sToken = "huawei:" + token;
        HuaweiToken.sendTokenToJS();
    }

    @Override
    public boolean onPushMsg(Context context, byte[] msgBytes, Bundle extras) {

        final String passThroughBody = new String(msgBytes, Charset.forName("UTF-8"));
        Log.d(LOGTAG, "New message from Huawei Push: " + passThroughBody);

        Bundle bundle = new Bundle();
        try {
            final JSONObject jsonObj = new JSONObject(passThroughBody);
            Iterator iterator = jsonObj.keys();
            while(iterator.hasNext()){
                String key = (String) iterator.next();//next方法，向下移动指针，并且返回指针指向的元素，如果指针指向的内存中没有元素，会报异常
                String value = jsonObj.getString(key);
                bundle.putString(key, value);
            }
        } catch (Throwable t) {
            Log.e(LOGTAG, "failed to convert pass through body from json", t);
            bundle.putString("title", passThroughBody);
        }

        // Object pushMsg = extras.get("pushMsg");
        Log.d(LOGTAG, "New message from Huawei: " + bundle.toString());

        try {
            final IPushNotification notification = PushNotification.get(HuaweiToken.mAppContext, bundle);
            if (notification != null) {
                notification.onReceived();
                return true;
            }
        } catch (IPushNotification.InvalidNotificationException e) {
            // A GCM message, yes - but not the kind we know how to work with.
            Log.v(LOGTAG, "Huawei Push message handling aborted", e);
        }

        return false;
        
    }

}
