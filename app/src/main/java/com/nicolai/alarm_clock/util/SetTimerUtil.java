package com.nicolai.alarm_clock.util;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.nicolai.alarm_clock.pojos.Timer_POJO;
import com.nicolai.alarm_clock.receiver_service.Timer_Receiver;

import java.util.Calendar;

public class SetTimerUtil {

    public static void set(Context context, Timer_POJO mTimer){
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        long timeToGoOff = Calendar.getInstance().getTimeInMillis() + mTimer.getMillisLeft();

        Intent alarmIntent = new Intent(context, Timer_Receiver.class);
        alarmIntent.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        alarmIntent.putExtra("ID", mTimer.getId());
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, mTimer.getId(), alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle( AlarmManager.RTC_WAKEUP, timeToGoOff, pendingIntent);
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, timeToGoOff, pendingIntent);
        }
    }

    public static void unSet(Context context, int id){
        Intent alarmIntent = new Intent(context, Timer_Receiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, id, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        //cancel the timer
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        pendingIntent.cancel();
        alarmManager.cancel(pendingIntent);
    }
}
