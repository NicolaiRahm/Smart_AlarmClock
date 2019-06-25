package com.nicolai.alarm_clock.receiver_service;

import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;
import com.nicolai.alarm_clock.util.AsyncHandler;
import com.nicolai.alarm_clock.util.NotificationUtil;

public class Timer_Receiver extends BroadcastReceiver{

    private int id;

    @Override
    public void onReceive(Context context, Intent intent) {

        if(intent.hasExtra("ID")){
            id = intent.getExtras().getInt("ID");

            AlarmAlertWakeLock.acquireScreenCpuWakeLock(context);

            Intent serviceIntent = new Intent(context, ForegroundService_TimerSound.class);
            serviceIntent.setAction(ForegroundService_TimerSound.ACTION_BROADCAST);
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
                mNotificationManager.notify(NotificationUtil.BROADCAST_TIMER, NotificationUtil.timerNotification(context, id));

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
                        NotificationUtil.cancelTimerNfc(context, NotificationUtil.BROADCAST_TIMER);
                        result.finish();
                    }
                }
            });
        }
    }
}
