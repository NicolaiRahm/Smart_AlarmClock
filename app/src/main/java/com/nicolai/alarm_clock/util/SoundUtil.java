package com.nicolai.alarm_clock.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.widget.SeekBar;

import static android.content.Context.AUDIO_SERVICE;

public class SoundUtil {

    public static int getDefaultVolume(Context context){
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        return (audioManager.getStreamMaxVolume(AudioManager.STREAM_RING) / 2) + 1 ;
    }

    public static int getMaxVolume(SeekBar seekBar){
        AudioManager audioManager = (AudioManager) seekBar.getContext().getSystemService(AUDIO_SERVICE);
        return audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
    }

    public static String soundTitle(Context context, String uri){
        return RingtoneManager.getRingtone(context, Uri.parse(uri)).getTitle(context);
    }

    public static String defaultUri(Context context, boolean alarmClock){
        SharedPreferences mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);

        Uri uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        String soundUri = uri.toString();

        if(alarmClock){
            soundUri = mSharedPreferences.getString("sound", soundUri);
        }else{
            soundUri = mSharedPreferences.getString("timer_sound", soundUri);
        }

        return soundUri;
    }

    public static void changeVolume(Context context, int volume){
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_ALARM, volume, 0);
    }

    public static float relativeVolumeMP(Context context, float volume){
        AudioManager audioManager = (AudioManager) context.getSystemService(AUDIO_SERVICE);
        return volume / audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
    }
}
