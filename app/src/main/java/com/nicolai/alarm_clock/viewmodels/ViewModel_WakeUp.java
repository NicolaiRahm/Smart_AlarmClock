package com.nicolai.alarm_clock.viewmodels;

import android.app.Application;

import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import com.nicolai.alarm_clock.room_database.AlarmRepository;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;

public class ViewModel_WakeUp extends AndroidViewModel {

    private AlarmRepository mRepository;
    private WeckerPOJO klingelderWecker;

    public ViewModel_WakeUp(@NonNull Application application, int id) {
        super(application);

        mRepository = new AlarmRepository(application);
        klingelderWecker = mRepository.getById(id);
    }

    public WeckerPOJO getKlingelderWecker(){
        return klingelderWecker;
    }

    public void deleteSafe(){
        mRepository.deleteSafe(klingelderWecker);
    }

    /*
    *Shared
    */

    public void messageTo_Minder_Sender(boolean appMessage, char iAmReceiver, String message){
        mRepository.wakeUp_shared(getApplication(), appMessage, iAmReceiver, klingelderWecker, message);
    }
}
