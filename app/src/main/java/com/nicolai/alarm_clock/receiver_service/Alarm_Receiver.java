package com.nicolai.alarm_clock.receiver_service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;
import com.nicolai.alarm_clock.util.AsyncHandler;
import com.nicolai.alarm_clock.util.NotificationUtil;

/*
 * Created by Nicolai on 02.07.2017.
 */

public class Alarm_Receiver extends BroadcastReceiver {
    private int id;

    @Override
    public void onReceive(final Context context, Intent intent) {
        id = intent.getExtras().getInt("ID");

        AlarmAlertWakeLock.acquireScreenCpuWakeLock(context);

        Intent serviceIntent = new Intent(context, ForegroundService_AlarmSound.class);
        serviceIntent.setAction(ForegroundService_AlarmSound.ACTION_SET_UP);
        serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        serviceIntent.putExtra("ID", id);

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
                    result.wait(3000);

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

        /*Intent wakeUp_intent = new Intent(context, WakeUp.class);
        wakeUp_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        wakeUp_intent.putExtra("ID", id);
        context.startActivity(wakeUp_intent);

        Intent jobService = new Intent();
        jobService.putExtra("ID", id);
        JobIntentService.enqueueWork(context, JobServiceStartAlarm.class, 999, jobService);

        final PendingResult result = goAsync();
        final Thread thread = new Thread() {
            public void run() {

                NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
                mNotificationManager.notify(NotificationUtil.RECEIVER_NOTIFICATION_ID, NotificationUtil.alarmNotification(context, id));

                synchronized (result){
                    try {
                        result.wait(3000);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }

                    Intent wakeUp_intent = new Intent(context, WakeUp.class);
                    wakeUp_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    wakeUp_intent.putExtra("ID", id);
                    context.startActivity(wakeUp_intent);

                    result.finish();
                }
            }
        };
        thread.start();*/
    }

    public Alarm_Receiver() {
        super();
    }
}