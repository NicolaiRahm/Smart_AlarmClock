package com.nicolai.alarm_clock;

import android.animation.ValueAnimator;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;

import com.nicolai.alarm_clock.adapter_helper.RecyclerItemTouchHelper_Timer;
import com.nicolai.alarm_clock.adapter_helper.RecyclerView_TimerAdapter;
import com.nicolai.alarm_clock.databinding.FragmentTimerBinding;
import com.nicolai.alarm_clock.dialog.NewTimerDF;
import com.nicolai.alarm_clock.pojos.Timer_POJO;
import com.nicolai.alarm_clock.receiver_service.ForegroundService_TimerSound;
import com.nicolai.alarm_clock.util.SetTimerUtil;
import com.nicolai.alarm_clock.viewmodels.ViewModel_MainTimer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimerTask;

import androidx.databinding.DataBindingUtil;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;


public class Timer extends Fragment implements View.OnClickListener, RecyclerItemTouchHelper_Timer.RecyclerItemTouchHelperListener{

    private FragmentTimerBinding mBinding;
    private ViewModel_MainTimer viewModel;
    private Timer_POJO currentTimer;
    private ValueAnimator valueAnimator;
    private RecyclerView_TimerAdapter mAdapter;

    private boolean isListUp;
    private List<Timer_POJO> timerList;

    private java.util.Timer mTimer;

    public Timer() {}

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mBinding = DataBindingUtil.inflate(LayoutInflater.from(getContext()), R.layout.fragment_timer, null, false);

        viewModel = ViewModelProviders.of(getActivity()).get(ViewModel_MainTimer.class);
        viewModel.getCurrentTimer().observe(this, timer ->{
            if(timer != null){
                setCurrentTimer(timer);
            }
        });

        mBinding.setLifecycleOwner(this);
        mBinding.fabStop.setOnClickListener(this);
        mBinding.fabStartPause.setOnClickListener(this);

        //LayoutManager for RecyclerView
        LinearLayoutManager mLinearLayoutManager = new LinearLayoutManager(getActivity(), RecyclerView.VERTICAL, false);

        //RecyclerView ListAdapter
        mAdapter = new RecyclerView_TimerAdapter(new RecyclerView_TimerAdapter.EditTimer() {
            @Override
            public void itemClicked(int position) {
                if(position == 0){
                    viewModel.setNewTimer(true);
                    NewTimerDF newTimerDF = new NewTimerDF();
                    newTimerDF.show(getFragmentManager(), "NewTimer");
                }else{
                    viewModel.setCurrentTimer(viewModel.getById(mAdapter.getID(position)));
                    viewModel.changeMotion();
                }
            }

            @Override
            public void onOffButton(final int position) {
                //viewModel.changeOn(mAdapter.getID(position));
            }
        });

        List<Timer_POJO> mockList = new ArrayList<Timer_POJO>();
        mockList.add(0, new Timer_POJO());
        mAdapter.submitList(mockList);

        //Set up RecyclerView
        //recyclerView.setHasFixedSize(true);
        mBinding.timerList.setAdapter(mAdapter);
        mBinding.timerList.setLayoutManager(mLinearLayoutManager);

        //For SwipeDelete to function
        ItemTouchHelper.SimpleCallback itemTouchHelperCallback = new RecyclerItemTouchHelper_Timer(0, ItemTouchHelper.LEFT, Timer.this);
        ItemTouchHelper swipeHelper = new ItemTouchHelper(itemTouchHelperCallback);
        swipeHelper.attachToRecyclerView(mBinding.timerList);

        //Get the data from the ViewModel
        viewModel.getAllTimers().observe(this, timers -> {
            getActivity().runOnUiThread(() -> {
                timers.add(0, new Timer_POJO());
                timerList = timers;

                if(isListUp){
                    mAdapter.submitList(timers);
                }
            });
        });

        //Fore the motion layout to respond to the fab in MainActivity
        viewModel.getMotion().observe(this, up -> {
            if(up){
                mBinding.motionLayoutTimer.transitionToEnd();
                mAdapter.submitList(timerList);
            }else {
                mBinding.motionLayoutTimer.transitionToStart();
            }

            isListUp = up;
        });

        mBinding.setViewModel(viewModel);

        mTimer = new java.util.Timer();

        return mBinding.getRoot();
    }

    private void setCurrentTimer(Timer_POJO timer){
        if((currentTimer != null && currentTimer.getId() != timer.getId()) || currentTimer == null){
            if((currentTimer != null && currentTimer.getId() != timer.getId()) && currentTimer.getState().isRunning()){
                SetTimerUtil.set(getContext(), currentTimer);
                mTimer.cancel();
                mTimer = null;
            }

            currentTimer = timer;
            if(timer.getState().isRunning()){
                currentTimer.setMillisLeft(currentTimer.getEnds() - Calendar.getInstance().getTimeInMillis());
                SetTimerUtil.unSet(getContext(), currentTimer.getId());
                startTimer(false);
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.fab_start_pause:
                if (currentTimer.getState().isRunning()) {
                    mTimer.cancel();
                    currentTimer.getState().setPaused();
                    currentTimer.setEnds(0L);
                    viewModel.setCurrentTimer(currentTimer);
                } else if(currentTimer.getState().isFinished()){
                    stopTimer();
                    startTimer(true);
                    fullScreenFinish();
                }else startTimer(true);
                break;
            case R.id.fab_stop: stopTimer(); fullScreenFinish(); break;
        }
    }

    public void fullScreenFinish(){
        if(viewModel.isFullScreen()){
            Handler handler = new Handler();
            handler.postDelayed(() -> {
                    getActivity().finish();
            }, 300);
        }
    }

    private void stopTimer(){
        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }

        if(valueAnimator != null) valueAnimator.cancel();
        mBinding.progressCountdown.setAlpha(1.0F);

        if(currentTimer.getState().isFinished()){
            Intent i = new Intent(getContext(), ForegroundService_TimerSound.class);
            i.setAction(ForegroundService_TimerSound.ACTION_STOP);
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                getContext().startForegroundService(i);
            }else{
                getContext().startService(i);
            }
        }

        currentTimer.reset();
        viewModel.setCurrentTimer(currentTimer);
    }

    private void startTimer(boolean button){
        if(button){
            currentTimer.getState().setRunning();
            currentTimer.setEnds(Calendar.getInstance().getTimeInMillis() + currentTimer.getMillisLeft());
            viewModel.setCurrentTimer(currentTimer);
        }

        mTimer = new java.util.Timer();
        mTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                currentTimer.updateMillisLeft(1000);
                viewModel.setCurrentTimerThread(currentTimer);

                if(currentTimer.getMillisLeft() <= 0 && currentTimer.getState().isRunning()){
                    currentTimer.getState().setFinished();
                    viewModel.setCurrentTimerThread(currentTimer);
                    finished();
                }

            }
        }, 0, 1000);
    }

    private void finished(){
        getActivity().runOnUiThread( () ->{
            //Ring animieren
            valueAnimator = ValueAnimator.ofFloat(1.0f, 0.0f, 1.0f);
            valueAnimator.addUpdateListener( animation ->  {
                float value = (Float) animation.getAnimatedValue();
                mBinding.progressCountdown.setAlpha(value);
            });

            valueAnimator.setInterpolator(new LinearInterpolator());
            valueAnimator.setDuration(2600);
            valueAnimator.setRepeatCount(100);
            valueAnimator.start();

            Intent i = new Intent(getContext(), ForegroundService_TimerSound.class);
            i.setAction(ForegroundService_TimerSound.ACTION_APP_IS_OPEN);
            i.putExtra("ID", currentTimer.getId());
            i.putExtra("volume", currentTimer.getVolume());
            i.putExtra("sound", currentTimer.getSound());
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
                getContext().startForegroundService(i);
            }else{
                getContext().startService(i);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();

        if(currentTimer != null){
            //In case the timer attributes have changed e.g. in full screen activity
            currentTimer = viewModel.getById(currentTimer.getId());

            if (currentTimer.getState().isRunning()){
                SetTimerUtil.unSet(getContext(), currentTimer.getId());
                currentTimer.setMillisLeft(currentTimer.getEnds() - Calendar.getInstance().getTimeInMillis());
                if(mTimer == null) startTimer(false);
            }else if(currentTimer.getState().isFinished()){
                finished();
                currentTimer.setMillisLeft(currentTimer.getEnds() - Calendar.getInstance().getTimeInMillis());
                if(mTimer == null) startTimer(false);
            }
            viewModel.setCurrentTimer(currentTimer);
        }
    }

    @Override
    public void onPause() {
        super.onPause();

        if(mTimer != null){
            mTimer.cancel();
            mTimer = null;
        }
        if (valueAnimator != null) valueAnimator.cancel();

        if(currentTimer != null && currentTimer.getState().isRunning()){
            SetTimerUtil.set(getContext(), currentTimer);
        }
    }

    @Override
    public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction, int position) {
        final int id = mAdapter.getID(position);
        if(id != currentTimer.getId()){
            SetTimerUtil.unSet(getContext(), id);
        }else if(currentTimer.getState().isRunning()){
            mTimer.cancel();
            mTimer = null;

            currentTimer = null;
        }

        viewModel.deleteById(id);
    }
}
