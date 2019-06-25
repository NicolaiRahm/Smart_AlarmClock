package com.nicolai.alarm_clock.viewmodels;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

public class FactoryWakeUp implements ViewModelProvider.Factory {

    private Application application;
    private int id;

    public FactoryWakeUp(Application application, int id){
        this.application = application;
        this.id = id;
    }

    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        return (T) new ViewModel_WakeUp(application, id);
    }
}
