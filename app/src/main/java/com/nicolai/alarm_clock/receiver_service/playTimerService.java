package com.nicolai.alarm_clock.receiver_service;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;

import com.nicolai.alarm_clock.util.SoundUtil;

public class playTimerService extends Service {
    private Ringtone ringtone;

    public playTimerService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        Uri alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);

        if(alert == null) {
            // alert is null, using backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);

            // I can't see this ever being null (as always have a default notification)
            // but just incase
        }else if(alert == null) {
            // alert backup is null, using 2nd backup
            alert = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getApplication());
        String costumeUri = mSharedPreferences.getString("timer_sound", alert.toString());

        ringtone = RingtoneManager.getRingtone(this, Uri.parse(costumeUri));

        //Starten des Rintones.play()
        if (Build.VERSION.SDK_INT >= 21) {
            AudioAttributes aa = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            ringtone.setAudioAttributes(aa);
        } else {
            ringtone.setStreamType(AudioManager.STREAM_ALARM);
        }
        ringtone.play();

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy(){
        ringtone.stop();
    }
}
