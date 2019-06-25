package com.nicolai.alarm_clock.receiver_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.nicolai.alarm_clock.WakeUp;
import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;

public class AlarmIntervallReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(final Context context, Intent intent) {
        AlarmAlertWakeLock.acquireScreenCpuWakeLock(context);

        Intent serviceIntent = new Intent(context, ForegroundService_AlarmSound.class);
        serviceIntent.setAction(ForegroundService_AlarmSound.ACTION_REPEAT);
        serviceIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            context.startForegroundService(serviceIntent);
        }else{
            context.startService(serviceIntent);
        }
    }
}
