package com.wix.reactnativenotifications.core.notificationdrawer;

import android.app.AlarmManager;
import android.app.Activity;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;

import com.wix.reactnativenotifications.core.AppLaunchHelper;
import com.wix.reactnativenotifications.core.InitialNotificationHolder;
import com.wix.reactnativenotifications.helpers.PushNotificationHelper;

import static com.wix.reactnativenotifications.Defs.LOGTAG;

public class PushNotificationsDrawer implements IPushNotificationsDrawer {
    final protected Context mContext;
    final protected AppLaunchHelper mAppLaunchHelper;

    public static IPushNotificationsDrawer get(Context context) {
        return PushNotificationsDrawer.get(context, new AppLaunchHelper());
    }

    public static IPushNotificationsDrawer get(Context context, AppLaunchHelper appLaunchHelper) {
        final Context appContext = context.getApplicationContext();
        if (appContext instanceof INotificationsDrawerApplication) {
            return ((INotificationsDrawerApplication) appContext).getPushNotificationsDrawer(context, appLaunchHelper);
        }

        return new PushNotificationsDrawer(context, appLaunchHelper);
    }

    protected PushNotificationsDrawer(Context context, AppLaunchHelper appLaunchHelper) {
        mContext = context;
        mAppLaunchHelper = appLaunchHelper;
    }

    @Override
    public void onAppInit() {
        clearAll();
    }

    @Override
    public void onAppVisible() {
        clearAll();
    }

    @Override
    public void onNewActivity(Activity activity) {
        boolean launchIntentsActivity = mAppLaunchHelper.isLaunchIntentsActivity(activity);
        boolean launchIntentOfNotification = mAppLaunchHelper.isLaunchIntentOfNotification(activity.getIntent());
        if (launchIntentsActivity && !launchIntentOfNotification) {
            InitialNotificationHolder.getInstance().clear();
        }
    }

    @Override
    public void onNotificationOpened() {
        clearAll();
    }

    @Override
    public void onNotificationClearRequest(int id) {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    @Override
    public void onCancelAllLocalNotifications() {
        clearAll();
        cancelAllScheduledNotifications();
    }

    protected void cancelAllScheduledNotifications() {
        Log.i(LOGTAG, "Cancelling all notifications");
        PushNotificationHelper helper = PushNotificationHelper.getInstance(mContext);

        for (String id : helper.getPreferencesKeys()) {
            cancelScheduledNotification(id);
        }
    }

    protected void cancelScheduledNotification(String notificationIDString) {
        Log.i(LOGTAG, "Cancelling notification: " + notificationIDString);

        PushNotificationHelper helper = PushNotificationHelper.getInstance(mContext);

        // remove it from the alarm manger schedule
        Bundle b = new Bundle();
        b.putString("id", notificationIDString);
        PendingIntent pendingIntent = helper.toScheduleNotificationIntent(b);
        helper.getAlarmManager().cancel(pendingIntent);

        helper.removePreference(notificationIDString);

        // removed it from the notification center
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(Integer.parseInt(notificationIDString));
    }

    protected void clearAll() {
        final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancelAll();
    }
}
