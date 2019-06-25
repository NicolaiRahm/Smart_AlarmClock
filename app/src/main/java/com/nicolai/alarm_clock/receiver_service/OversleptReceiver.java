package com.nicolai.alarm_clock.receiver_service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;
import com.nicolai.alarm_clock.util.AsyncHandler;
import com.nicolai.alarm_clock.util.NotificationUtil;

public class OversleptReceiver extends BroadcastReceiver {

    private int id;

    @Override
    public void onReceive(final Context context, Intent intent) {
        AlarmAlertWakeLock.acquireScreenCpuWakeLock(context);

        id = intent.getExtras().getInt("ID");

        Intent serviceIntent = new Intent(context, ForegroundService_AlarmSound.class);
        serviceIntent.setAction(ForegroundService_AlarmSound.ACTION_OVERSLEPT);
        serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(serviceIntent);
        }else{
            context.startService(serviceIntent);
        }

        final PendingResult result = goAsync();
        AsyncHandler.post(() -> {
            //Bisschen rumspielen damit anderer Intent vlt besser durch kommt ohne sleep oder memory issues
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            mNotificationManager.notify(NotificationUtil.RECEIVER_NOTIFICATION_ID, NotificationUtil.alarmNotification(context, id));

            synchronized (result){
                try {
                    result.wait(2000);

                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                        context.startForegroundService(serviceIntent);
                    }else{
                        context.startService(serviceIntent);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }finally {
                    NotificationUtil.cancelAlarmNfc(context, NotificationUtil.RECEIVER_NOTIFICATION_ID);
                    result.finish();
                }
            }
        });


    }
}
