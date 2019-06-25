package com.nicolai.alarm_clock.receiver_service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;

import com.nicolai.alarm_clock.WakeUp;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.room_database.AlarmRepository;
import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;
import com.nicolai.alarm_clock.util.AlarmUtil;
import com.nicolai.alarm_clock.util.NotificationUtil;
import com.nicolai.alarm_clock.util.SoundUtil;

import java.io.IOException;

public class ForegroundService_AlarmSound extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener{

    public static final String ACTION_SET_UP = "SET_UP";
    public static final String ACTION_START = "START";
    public static final String ACTION_REPEAT = "REPEAT";
    public static final String ACTION_SNOOZE = "SNOOZE";
    public static final String ACTION_OVERSLEPT = "OVERSLEPT";
    public static final String ACTION_STOP = "STOP";

    private MediaPlayer mp;
    private Handler stopAfterDuration = new Handler();
    private WeckerPOJO klingelnderWecker;

    private int songLength;
    private int alarmCount = 1;
    private boolean alreadyStarted, alreadySetUp, alreadyOverslept;

    public ForegroundService_AlarmSound() {}

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if(intent != null && intent.getAction() != null) {
            String action = intent.getAction();

            switch (action) {
                case ACTION_SET_UP:
                    if(!alreadySetUp){
                        alreadySetUp = true;

                        AlarmRepository mRepository = new AlarmRepository(getApplication());
                        klingelnderWecker = mRepository.getById(intent.getIntExtra("ID", 0));
                        startForegroundService();
                        intent("");
                    }
                    break;
                case ACTION_START:
                    if(!alreadyStarted && klingelnderWecker != null){
                        SoundUtil.changeVolume(getApplicationContext(), klingelnderWecker.getVolume());
                        setUpMediaPlayer(SoundUtil.relativeVolumeMP(getApplicationContext(), (float) klingelnderWecker.getVolume()));
                        setOversleptAlarm();

                        if(alarmCount < klingelnderWecker.getAnzahl() && klingelnderWecker.getIntervall() != 0) {
                            //Calendar --> Alarmmanager an Broadcastreceiver (AlarmIntervallReceiver)
                            AlarmUtil.setRepeating(getApplicationContext(), klingelnderWecker.getId(), klingelnderWecker.getIntervall());
                        }

                        alreadyStarted = true;
                    }
                    break;
                case ACTION_REPEAT:
                    if(alreadyStarted && klingelnderWecker != null){
                        alarmCount += 1;
                        setUpMediaPlayer(SoundUtil.relativeVolumeMP(getApplicationContext(), (float) klingelnderWecker.getVolume()));
                        intent(AlarmUtil.REPEAT);
                    }
                    break;
                case ACTION_SNOOZE: snooze(intent.getLongExtra("snoozeDuartion", AlarmUtil.FIVE_MIN_SNOOZE)); break;
                case ACTION_OVERSLEPT:
                    if(!alreadyOverslept) {
                    intent(AlarmUtil.OVERSLEPT);
                    alreadyOverslept = true; }
                    break;
                case ACTION_STOP: stopForegroundService(); break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startForegroundService() {
        startForeground(NotificationUtil.SERVICE_NOTIFICATION_ID,
                NotificationUtil.alarmNotification(getApplicationContext(), klingelnderWecker.getId()));
    }

    private void intent(String action){
        if(klingelnderWecker != null){//TODO schlechte lösung für micht gelöschten overslept alarm
            //TODO So bleibt bei absturz die notification von overslept receiver stehen!!!!!!!!!!!!!!!!
            Intent wakeUp_intent = new Intent(getApplicationContext(), WakeUp.class);
            wakeUp_intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            wakeUp_intent.putExtra(AlarmUtil.ALARM_COUNT, alarmCount);
            wakeUp_intent.putExtra("ID", klingelnderWecker.getId());
            wakeUp_intent.setAction(action);
            getApplication().startActivity(wakeUp_intent);
        }
    }

    private void stopForegroundService() {
        if(klingelnderWecker != null){
            killMP_Handler();
            AlarmUtil.cancelOverslept(getApplication(), klingelnderWecker.getId());
            AlarmAlertWakeLock.releaseCpuLock();
        }

        stopForeground(true);
        stopSelf();
    }

    public void setUpMediaPlayer(float volume){
        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_ALARM);
        //mp.setLooping(true);
        mp.setVolume(volume, volume);
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mp.setOnErrorListener(this);
        mp.setOnPreparedListener(this);
        try {
            mp.setDataSource(getApplicationContext(), Uri.parse(klingelnderWecker.getAlarm_sound()));
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void snooze(long snoozeDuration){
        killMP_Handler();

        if(snoozeDuration != 0L){
            if(klingelnderWecker.getDuration() == 90){
                long time = snoozeDuration + songLength + klingelnderWecker.getSeconds_bis_verschlafen()*1000;
                AlarmUtil.setOverslept(getApplication(), klingelnderWecker.getId(), time);
            }else{
                long time = snoozeDuration + klingelnderWecker.getDuration() + klingelnderWecker.getSeconds_bis_verschlafen()*1000;
                AlarmUtil.setOverslept(getApplication(), klingelnderWecker.getId(), time);
            }
        }
    }

    private void setOversleptAlarm(){
        if(klingelnderWecker.getAnzahl() == 1){
            //Wenn volle Songlänge
            if(klingelnderWecker.getDuration() == 90){ //(111 = loop gibts noch garnicht als knopf)
                //TODO auch für Titel in AlarmClock!!
                Uri uri = Uri.parse(klingelnderWecker.getAlarm_sound());
                MediaMetadataRetriever mmr = new MediaMetadataRetriever();
                mmr.setDataSource(this,uri);
                String durationStr = mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                songLength = Integer.parseInt(durationStr);
                //Falls kürzer als eine min
                if(songLength < 60000){
                    songLength = 600000;
                }

                long time = songLength + klingelnderWecker.getSeconds_bis_verschlafen()*1000;
                AlarmUtil.setOverslept(getApplication(), klingelnderWecker.getId(), time);
            }else{
                long time = klingelnderWecker.getDuration() * 1000 + klingelnderWecker.getSeconds_bis_verschlafen()*1000;
                AlarmUtil.setOverslept(getApplication(), klingelnderWecker.getId(), time);
            }

        }else{
            long time = klingelnderWecker.getIntervall() * (klingelnderWecker.getAnzahl() - 1) + klingelnderWecker.getSeconds_bis_verschlafen()*1000;
            AlarmUtil.setOverslept(getApplication(), klingelnderWecker.getId(), time);
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
        setDurationMP();
    }

    private void setDurationMP(){
        //Nach duration automatisch stoppen
        Runnable runnable = () -> {
            if (mp != null && mp.isPlaying()){
                mp.stop();
                mp.release();
                mp = null;
            }
        };

        //Bei 90:ganzer Song
        //Bei 111:Loop
        int duration = klingelnderWecker.getDuration();

        if((duration == 90 || duration == 111) && klingelnderWecker.getIntervall() != 0 && alarmCount < klingelnderWecker.getAnzahl()){
            stopAfterDuration.postDelayed(runnable, klingelnderWecker.getIntervall()-4000);
        }else if(duration != 90 && duration != 111){
            stopAfterDuration.postDelayed(runnable, duration * 1000 - 2000);
        }else if(duration == 111){
            mp.setLooping(true);
        }
    }

    private void killMP_Handler(){
        stopAfterDuration.removeCallbacks(null);

        if (mp != null){
            mp.stop();
            mp.release();
            mp = null;
        }
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        //TODO vlt. da einsteigen wo aufgehört
        mp.reset();
        mp.release();
        mp = null;
        setUpMediaPlayer(SoundUtil.relativeVolumeMP(getApplicationContext(), (float) klingelnderWecker.getVolume()));

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        killMP_Handler();
        AlarmAlertWakeLock.releaseCpuLock();
    }
}
