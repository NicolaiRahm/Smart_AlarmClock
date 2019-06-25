package com.nicolai.alarm_clock.receiver_service;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;

import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.util.AsyncHandler;

public class Boot extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        String action = intent.getAction();

        final PendingResult result = goAsync();
        final PowerManager.WakeLock wl = AlarmAlertWakeLock.createPartialWakeLock(context);
        wl.acquire();

        AsyncHandler.post(() -> {
            try {
                if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    // All N devices have split storage areas, but we may need to
                    // move the existing preferences to the new device protected
                    // storage area, which is where the data lives from now on.

                    final Context deviceContext = context.createDeviceProtectedStorageContext();
                    //true if the move was successful or if the database didn't exist in the source context, otherwise false.
                    //--> wird nur ausgeführt wenn auf >= 24 geupdated wird
                    deviceContext.moveDatabaseFrom(context, WeckerDatabase.DATABASE_NAME);

                    //AlarmRoomDatabase.class uses context.createDeviceProtectedStorageContext() for >= Version 24
                    AlarmUtil.setAfterBoot(context);
                } else if(Intent.ACTION_BOOT_COMPLETED.equals(action) /*&& Build.VERSION.SDK_INT < Build.VERSION_CODES.N*/){
                    AlarmUtil.setAfterBoot(context);
                }
            } finally {
                result.finish();
                wl.release();
            }
        });
    }
}