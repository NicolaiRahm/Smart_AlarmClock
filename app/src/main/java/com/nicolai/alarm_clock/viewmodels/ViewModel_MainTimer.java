package com.nicolai.alarm_clock.viewmodels;

import android.app.Application;
import android.text.Editable;
import android.widget.SeekBar;

import com.nicolai.alarm_clock.pojos.Timer_POJO;
import com.nicolai.alarm_clock.room_database.TimerRepository;
import com.nicolai.alarm_clock.util.SoundUtil;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class ViewModel_MainTimer extends AndroidViewModel {

    private TimerRepository mRepository;
    private MutableLiveData<Timer_POJO> currentTimer = new MutableLiveData<>();
    private MutableLiveData<Boolean> changeMotion = new MutableLiveData<>();
    private MutableLiveData<Boolean> chooseRingtone = new MutableLiveData<>();
    private MutableLiveData<String> soundLive = new MutableLiveData<>();
    private Timer_POJO newEditTimer;
    private boolean newTimer, fullScreen;

    public ViewModel_MainTimer(@NonNull Application application) {
        super(application);

        mRepository = new TimerRepository(application);
        changeMotion.setValue(false);
        currentTimer.setValue(mRepository.latestTimer());
        newEditTimer = new Timer_POJO(getApplication());
        soundLive.setValue(SoundUtil.soundTitle(getApplication(), newEditTimer.getSound()));
        chooseRingtone.setValue(false);
    }

    public LiveData<List<Timer_POJO>> getAllTimers(){
        return mRepository.getAllTimers();
    }

    public LiveData<Timer_POJO> getCurrentTimer(){
        return currentTimer;
    }

    public void setCurrentTimerThread(Timer_POJO chosenTimer){
        currentTimer.postValue(chosenTimer);
        if (chosenTimer != null) mRepository.update(chosenTimer);
    }
    public void setCurrentTimer(Timer_POJO chosenTimer){
        currentTimer.setValue(chosenTimer);
        if (chosenTimer != null) mRepository.update(chosenTimer);
    }

    public Timer_POJO getById(int id){
        return mRepository.getById(id);
    }

    public void insert(){
        int id = (int) mRepository.insert(newEditTimer);
        currentTimer.postValue(mRepository.getById(id));
        changeMotion();
    }

    public void update(){
        mRepository.update(newEditTimer);
    }

    public void deleteById(int id){
        mRepository.deleteById(id);
        currentTimer.setValue(mRepository.latestTimer());
    }

    public LiveData<Boolean> getMotion(){
        return changeMotion;
    }

    public void changeMotion(){
        changeMotion.setValue(!changeMotion.getValue());
    }

    public Timer_POJO getNewEditTimer(){
        return newEditTimer;
    }

    public void setNewEditTimer(Timer_POJO timer){
        newEditTimer = timer;
        soundLive.setValue(SoundUtil.soundTitle(getApplication(), newEditTimer.getSound()));
    }

    public boolean isNewTimer() {
        return newTimer;
    }

    public void setNewTimer(boolean newTimer) {
        if(newTimer){
            newEditTimer = new Timer_POJO(getApplication());
            soundLive.setValue(SoundUtil.soundTitle(getApplication(), newEditTimer.getSound()));
        }
        this.newTimer = newTimer;
    }

    public boolean isFullScreen() {
        return fullScreen;
    }

    public void setFullScreen(boolean fullScreen){
        this.fullScreen = fullScreen;
    }



    public void setVolume(SeekBar seekBar, int progress, boolean fromUser){
        newEditTimer.setVolume(progress);
    }

    public void setName(Editable s){
        newEditTimer.setName(s.toString());
    }

    public void setDuration(long duration){
        newEditTimer.setDuration(duration);
        if(newEditTimer.getState().isIdle()) newEditTimer.setMillisLeft(duration);
    }

    public void setSound(String sound){
        newEditTimer.setSound(sound);
        soundLive.setValue(SoundUtil.soundTitle(getApplication(), sound));
    }

    public LiveData<String> soundLive(){
        return soundLive;
    }

    //Choose ringtone

    public LiveData<Boolean> isChooseRingtone() {
        return chooseRingtone;
    }

    public void setChooseRingtone(boolean chooseRingtone) {
        this.chooseRingtone.setValue(chooseRingtone);
    }
}
