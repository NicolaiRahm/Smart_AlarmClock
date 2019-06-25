package com.nicolai.alarm_clock.viewmodels;

import com.nicolai.alarm_clock.pojos.WeckerPOJO;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class FactoryTimeseter extends ViewModelProvider.NewInstanceFactory {

    private final WeckerPOJO currentWecker;
    private final boolean neu, assisted;

    public FactoryTimeseter(WeckerPOJO currentWecker, boolean neu, boolean assisted){
        this.currentWecker = currentWecker;
        this.neu = neu;
        this.assisted = assisted;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ViewModel_TimeSeter(currentWecker, neu, assisted);
    }
}
