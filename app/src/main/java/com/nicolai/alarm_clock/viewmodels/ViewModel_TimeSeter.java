package com.nicolai.alarm_clock.viewmodels;

import com.nicolai.alarm_clock.pojos.WeckerPOJO;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class ViewModel_TimeSeter extends ViewModel {
    private MutableLiveData<WeckerPOJO> currentWecker = new MutableLiveData<WeckerPOJO>();

    private final boolean neu, assisted;

    public ViewModel_TimeSeter(WeckerPOJO currentWecker, boolean neu, boolean assisted){
        this.currentWecker.setValue(currentWecker);
        this.neu = neu;
        this.assisted = assisted;
    }

    public MutableLiveData<WeckerPOJO> getCurrentWecker() {
        return currentWecker;
    }

   /* public void setCurrentWecker(LiveData<WeckerPOJO> currentWecker) {
        this.currentWecker = currentWecker;
    }*/

    public boolean isNeu(){
        return neu;
    }

    public boolean isAssisted(){
        return assisted;
    }
}
