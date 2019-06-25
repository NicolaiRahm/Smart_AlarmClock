package com.nicolai.alarm_clock.util;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.nicolai.alarm_clock.R;
import com.nicolai.alarm_clock.TimerFullScreen;
import com.nicolai.alarm_clock.WakeUp;

import androidx.core.app.NotificationCompat;

import static androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC;

public class NotificationUtil {
    public static final int RECEIVER_NOTIFICATION_ID = 2;
    public static final int SERVICE_NOTIFICATION_ID = 3;

    public static final int BROADCAST_TIMER = 4;
    public static final int SERVICE_TIMER = 5;

    public static Notification alarmNotification(Context context, int id){

        Intent notifyIntent = new Intent(context, WakeUp.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifyIntent.putExtra("ID", id);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(context, 3030, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Channel
            CharSequence name = context.getString(R.string.alarm_notification_name);
            String description = context.getString(R.string.alarm_notification_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("alarm", name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
            channel.setSound(null, null);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(context, "alarm")
                    .setContentTitle(context.getString(R.string.alarm_notification_title))
                    .setContentText(context.getString(R.string.alarm_notification_text))
                    .setSmallIcon(R.drawable.ic_access_alarm_white_24dp)
                    //.setTicker(getText(R.string.ticker_text))
                    .setContentIntent(notifyPendingIntent)
                    .setAutoCancel(true)
                    .build();

            return notification;

        }else{

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, null)
                    .setContentTitle(context.getString(R.string.alarm_notification_title))
                    .setSmallIcon(R.drawable.ic_access_alarm_white_24dp)
                    .setContentText(context.getString(R.string.alarm_notification_text))
                    .setContentIntent(notifyPendingIntent)
                    .setAutoCancel(true);

            return mBuilder.build();
        }
    }

    public static void cancelAlarmNfc(Context context, int notificationId){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }

    public static Notification timerNotification(Context context, int id){

        Intent notifyIntent = new Intent(context, TimerFullScreen.class);
        notifyIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        notifyIntent.putExtra("ID", id);
        PendingIntent notifyPendingIntent = PendingIntent.getActivity(context, 564, notifyIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            //Channel
            CharSequence name = context.getString(R.string.timer_notification_name);
            String description = context.getString(R.string.timer_notification_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel("timer", name, importance);
            channel.setDescription(description);
            channel.setLockscreenVisibility(VISIBILITY_PUBLIC);
            channel.setSound(null, null);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);

            Notification notification = new Notification.Builder(context, "timer")
                    .setContentTitle(context.getString(R.string.timer_notification_title))
                    .setContentText(context.getString(R.string.timer_notification_text))
                    .setSmallIcon(R.drawable.ic_access_alarm_white_24dp)
                    //.setTicker(getText(R.string.ticker_text))
                    .setContentIntent(notifyPendingIntent)
                    .setAutoCancel(true)
                    .build();

            return notification;

        }else{

            NotificationCompat.Builder mBuilder = new NotificationCompat.Builder(context, null)
                    .setContentTitle(context.getString(R.string.timer_notification_title))
                    .setSmallIcon(R.drawable.ic_access_alarm_white_24dp)
                    .setContentText(context.getString(R.string.timer_notification_text))
                    .setContentIntent(notifyPendingIntent)
                    .setAutoCancel(true);

            return mBuilder.build();
        }
    }

    public static void cancelTimerNfc(Context context, int notificationId){
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancel(notificationId);
    }
}
