package com.nicolai.alarm_clock.receiver_service;

import android.app.Service;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.IBinder;
import android.os.PowerManager;

import com.nicolai.alarm_clock.TimerFullScreen;
import com.nicolai.alarm_clock.pojos.Timer_POJO;
import com.nicolai.alarm_clock.room_database.TimerRepository;
import com.nicolai.alarm_clock.util.AlarmAlertWakeLock;
import com.nicolai.alarm_clock.util.NotificationUtil;
import com.nicolai.alarm_clock.util.SoundUtil;

import java.io.IOException;

public class ForegroundService_TimerSound extends Service implements MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener{

    public static final String ACTION_BROADCAST = "ALRAM_RECEIVER";
    public static final String ACTION_APP_IS_OPEN = "APP_IS_OPEN";
    public static final String ACTION_START = "START";
    public static final String ACTION_STOP = "STOP";

    private MediaPlayer mp;
    private float volume;
    private String sound;
    private int id = -10;

    private boolean alreadySetUp, alreadyStarted;

    public ForegroundService_TimerSound() { }

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
                case ACTION_BROADCAST:
                    if(!alreadySetUp || (id != -10 && id != intent.getIntExtra("ID", -10))){
                        //Another timer was started -> reset first one
                        if(id != -10 && id != intent.getIntExtra("ID", -10)){
                            TimerRepository mRepository = new TimerRepository(getApplication());
                            Timer_POJO currentTimer = mRepository.getById(id);
                            currentTimer.reset();
                            mRepository.update(currentTimer);
                        }

                        alreadyStarted = false;
                        alreadySetUp = true;
                        id = intent.getIntExtra("ID", -10);
                        startForegroundService(id);

                        Intent i = new Intent(getApplicationContext(), TimerFullScreen.class);
                        i.addFlags(Intent.FLAG_RECEIVER_FOREGROUND | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
                        i.putExtra("ID", id);
                        getApplication().startActivity(i);
                    }
                    break;

                case ACTION_APP_IS_OPEN:
                    if(!alreadySetUp || (id != -10 && id != intent.getIntExtra("ID", -10))){
                        alreadySetUp = true;
                        alreadyStarted = true;

                        //Another timer was started -> reset first one
                        if(id != -10 && id != intent.getIntExtra("ID", -10)){
                            TimerRepository mRepository = new TimerRepository(getApplication());
                            Timer_POJO currentTimer = mRepository.getById(id);
                            currentTimer.reset();
                            mRepository.update(currentTimer);
                        }

                        id = intent.getIntExtra("ID", 0);
                        startForegroundService(id);

                        volume = intent.getFloatExtra("volume", 3);
                        SoundUtil.changeVolume(getApplicationContext(), (int) volume);
                        sound = intent.getStringExtra("sound");
                        setUpMediaPlayer(sound);
                    }

                case ACTION_START:
                    if(!alreadyStarted){
                        alreadyStarted = true;

                        volume = intent.getFloatExtra("volume", 3);
                        SoundUtil.changeVolume(getApplicationContext(), (int) volume);
                        sound = intent.getStringExtra("sound");
                        setUpMediaPlayer(sound);
                    }
                    break;

                case ACTION_STOP:
                    stopForegroundService();
                    break;
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void startForegroundService(int id) {
        startForeground(NotificationUtil.SERVICE_TIMER, NotificationUtil.timerNotification(getApplicationContext(), id));
    }

    private void stopForegroundService() {

        if(mp != null){
            mp.stop();
            mp.release();
            mp = null;
        }

        AlarmAlertWakeLock.releaseCpuLock();

        stopForeground(true);
        stopSelf();
    }

    public void setUpMediaPlayer(String sound){

        //In case another timer goes off
        if(mp != null){
            mp.stop();
            mp.release();
            mp = null;
        }

        mp = new MediaPlayer();
        mp.setAudioStreamType(AudioManager.STREAM_ALARM);
        //mp.setVolume(volume / SoundUtil.maxVolume(getApplicationContext()), SoundUtil.maxVolume(getApplicationContext()));
        mp.setLooping(true);
        mp.setWakeMode(getApplicationContext(), PowerManager.PARTIAL_WAKE_LOCK);
        mp.setOnErrorListener(this);
        mp.setOnPreparedListener(this);
        try {
            mp.setDataSource(getApplicationContext(), Uri.parse(sound));
            mp.prepareAsync();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        mp.start();
    }

    @Override
    public boolean onError(MediaPlayer mediaPlayer, int what, int extra) {
        //TODO vlt. da einsteigen wo aufgehört
        mp.reset();
        mp.release();
        mp = null;
        setUpMediaPlayer(sound);

        return false;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mp != null){
            mp.release();
        }

        AlarmAlertWakeLock.releaseCpuLock();
    }
}
