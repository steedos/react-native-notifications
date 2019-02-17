package com.wix.reactnativenotifications.helpers;

import android.app.AlarmManager;
import android.os.Build;
import android.os.Bundle;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.content.SharedPreferences;
import android.util.Log;

import com.wix.reactnativenotifications.core.notification.PushNotificationProps;
import com.wix.reactnativenotifications.core.notification.PushNotificationPublisher;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

public class PushNotificationHelper {
    public static PushNotificationHelper sInstance;
    public static final String PREFERENCES_KEY = "rn_push_notification";
    static final String NOTIFICATION_ID = "notificationId";

    private final SharedPreferences scheduledNotificationsPersistence;
    protected final Context mContext;

    private PushNotificationHelper(Context context) {
        this.mContext = context;
        this.scheduledNotificationsPersistence = context.getSharedPreferences(PushNotificationHelper.PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    public static PushNotificationHelper getInstance(Context context) {
        if (sInstance == null) {
            sInstance = new PushNotificationHelper(context);
        }
        return sInstance;
    }

    public AlarmManager getAlarmManager() {
        return (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
    }

    public PendingIntent toScheduleNotificationIntent(Bundle bundle) {
        Integer notificationId = Integer.valueOf(bundle.getString("id"));
        Intent notificationIntent = new Intent(mContext, PushNotificationPublisher.class);
        notificationIntent.putExtra(PushNotificationHelper.NOTIFICATION_ID, notificationId);
        notificationIntent.putExtras(bundle);
        return PendingIntent.getBroadcast(mContext, notificationId.intValue(), notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
    }

    public boolean savePreferences(String id, PushNotificationProps notificationProps) {
        SharedPreferences.Editor editor = scheduledNotificationsPersistence.edit();
        editor.putString(id, notificationProps.toString());
        commit(editor);

        return scheduledNotificationsPersistence.contains(id);
    }

    public void removePreference(String notificationIDString) {
        if (scheduledNotificationsPersistence.contains(notificationIDString)) {
            // remove it from local storage
            SharedPreferences.Editor editor = scheduledNotificationsPersistence.edit();
            editor.remove(notificationIDString);
            commit(editor);
        } else {
            Log.w(LOGTAG, "Unable to find notification " + notificationIDString);
        }
    }

    public java.util.Set<String> getPreferencesKeys() {
        return scheduledNotificationsPersistence.getAll().keySet();
    }

    private static void commit(SharedPreferences.Editor editor) {
        if (Build.VERSION.SDK_INT < 9) {
            editor.commit();
        } else {
            editor.apply();
        }
    }
}