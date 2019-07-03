package com.wix.reactnativenotifications.xiaomi;

import java.util.List;

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

import com.xiaomi.mipush.sdk.ErrorCode;
import com.xiaomi.mipush.sdk.MiPushClient;
import com.xiaomi.mipush.sdk.MiPushCommandMessage;
import com.xiaomi.mipush.sdk.MiPushMessage;
import com.xiaomi.mipush.sdk.PushMessageReceiver;

public class XiaomiInstanceIdListenerService extends PushMessageReceiver {


    @Override
    public void onCommandResult(Context context, MiPushCommandMessage message) {

        String command = message.getCommand();
        List<String> arguments = message.getCommandArguments();
        String cmdArg1 = ((arguments != null && arguments.size() > 0) ? arguments.get(0) : null);
        String cmdArg2 = ((arguments != null && arguments.size() > 1) ? arguments.get(1) : null);
        String log;

        Log.d(LOGTAG, "Xiaomi Push onCommandResult " + command);

        int commandType = -1;
        if (MiPushClient.COMMAND_REGISTER.equals(command)) {
            if (message.getResultCode() == ErrorCode.SUCCESS) {
                String mRegId = cmdArg1;

                XiaomiToken.sToken = "xiaomi:" + mRegId;
                XiaomiToken.sendTokenToJS();
                Log.d(LOGTAG, "Xiaomi Push GetToken success: " + mRegId);
            } else {
                Log.e(LOGTAG, "Xiaomi Push GetToken failed.");
            }
        }
    }

    public void processNotificationMessage(String passThroughBody) { 
    
        Log.d(LOGTAG, "Xiaomi Push new message: " + passThroughBody);

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

        try {
            final IPushNotification notification = PushNotification.get(XiaomiToken.mAppContext, bundle);
            if (notification != null) {
                notification.onReceived();
            }
        } catch (IPushNotification.InvalidNotificationException e) {
            // A GCM message, yes - but not the kind we know how to work with.
            Log.v(LOGTAG, "Xiaomi Push message handling aborted", e);
        }
        
    }

    @Override
    public void onNotificationMessageArrived(Context context, MiPushMessage message) {
        processNotificationMessage(message.getContent());
    }

    @Override
    public void onReceivePassThroughMessage(Context context, MiPushMessage message) { 
        processNotificationMessage(message.getContent());
    }
}