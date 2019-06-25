package com.nicolai.alarm_clock.viewmodels;

import android.app.Application;

import com.nicolai.alarm_clock.room_database.AlarmRepository;
import com.nicolai.alarm_clock.room_database.WeckerDatabase;
import com.nicolai.alarm_clock.pojos.WeckerPOJO;

import java.util.List;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

public class MainViewModel extends AndroidViewModel {

    private AlarmRepository mRepository;
    private LiveData<List<WeckerPOJO>> liveWeckers;

    public MainViewModel(@NonNull Application application){
        super(application);

        mRepository = new AlarmRepository(application);
        liveWeckers = mRepository.getAllAlarms();
    }

    public LiveData<List<WeckerPOJO>> getLiveWeckers() {
        return liveWeckers;
    }

    public WeckerPOJO getById(int id){
        return mRepository.getById(id);
    }

    public void update(WeckerPOJO alarm){
        mRepository.update(alarm);
    }

    public long insert(WeckerPOJO newAlarm){
        return mRepository.insert(newAlarm);
    }

    public List<WeckerPOJO> getSharedList(){
        return mRepository.getBootList();
    }

    public void deleteById(int id){
        mRepository.deleteById(id);
    }

    /*
     **************************************** FIREBASE ACTION ***************************************
     */

    public void messagesDeletedFld(int id){
        mRepository.messagesDeletedFld(id);
    }

    public void alarmApproved(String sender_id, String sender_alarm_id){
        mRepository.alarmApproved(sender_id, sender_alarm_id);
    }

    public void fullVersion(){
        mRepository.fullVersion();
    }
}
