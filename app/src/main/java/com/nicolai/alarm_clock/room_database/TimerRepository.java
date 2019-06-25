package com.nicolai.alarm_clock.room_database;

import android.content.Context;
import android.os.AsyncTask;

import com.nicolai.alarm_clock.pojos.Timer_POJO;

import java.util.List;
import androidx.lifecycle.LiveData;

public class TimerRepository {
    private Timer_Dao mTimerDao;

    public TimerRepository(Context context){
        TimerRoomDatabase db = TimerRoomDatabase.getDatabase(context);
        mTimerDao = db.timerDao();
    }

    public LiveData<List<Timer_POJO>> getAllTimers() {
        return mTimerDao.getAllTimers();
    }

    //Get the id of the latest timer to display as default in fragment Timer
    public Timer_POJO latestTimer(){
        latestTimerAsyncTask asyncTask = new latestTimerAsyncTask(mTimerDao);
        try {
            return asyncTask.execute().get();
        }catch (java.util.concurrent.ExecutionException e){
            return null;
        }catch (InterruptedException e){
            return null;
        }
    }

    private static class latestTimerAsyncTask extends AsyncTask<Void, Void, Timer_POJO>{

        private Timer_Dao mAsyncTaskDao;

        latestTimerAsyncTask(Timer_Dao dao){
            mAsyncTaskDao = dao;
        }

        @Override
        protected Timer_POJO doInBackground(final Void... params) {
            return mAsyncTaskDao.latestTimer();
        }
    }

    //Boot list
    public List<Timer_POJO> getBootList () {
        bootListAsyncTask asyncTask = new bootListAsyncTask(mTimerDao);
        try {
            return asyncTask.execute().get();
        }catch (java.util.concurrent.ExecutionException e){
            return null;
        }catch (InterruptedException e){
            return null;
        }
    }

    private static class bootListAsyncTask extends AsyncTask<Void, Void, List<Timer_POJO>> {

        private Timer_Dao mAsyncTaskDao;

        bootListAsyncTask(Timer_Dao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected List<Timer_POJO> doInBackground(final Void... params) {
            return mAsyncTaskDao.getBootList();
        }
    }


    //Insert new alarm clock
    public long insert (Timer_POJO alarm) {
        TimerRepository.insertAsyncTask asyncTask = new TimerRepository.insertAsyncTask(mTimerDao);
        try {
            return asyncTask.execute(alarm).get();
        }catch (java.util.concurrent.ExecutionException e){
            return 0;
        }catch (InterruptedException e){
            return 0;
        }
    }

    private static class insertAsyncTask extends AsyncTask<Timer_POJO, Void, Long> {

        private Timer_Dao mAsyncTaskDao;

        insertAsyncTask(Timer_Dao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Long doInBackground(final Timer_POJO... params) {
            return mAsyncTaskDao.insert(params[0]);
        }
    }


    //delete by id
    public void deleteById(int id){
        new TimerRepository.deleteByIdAsyncTask(mTimerDao).execute(id);
    }

    private static class deleteByIdAsyncTask extends AsyncTask<Integer, Void, Void> {

        private Timer_Dao mAsyncTaskDao;

        deleteByIdAsyncTask(Timer_Dao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Integer... params) {
            mAsyncTaskDao.deleteById(params[0]);
            return null;
        }
    }


    //Get by id
    public Timer_POJO getById(int id){
        TimerRepository.getByIdAsyncTask alarm =  new TimerRepository.getByIdAsyncTask(mTimerDao);
        try {
            return alarm.execute(id).get();
        }catch (java.util.concurrent.ExecutionException e){
            return null;
        }catch (InterruptedException e){
            return null;
        }
    }

    private static class getByIdAsyncTask extends AsyncTask<Integer, Void, Timer_POJO> {

        private Timer_Dao mAsyncTaskDao;

        getByIdAsyncTask(Timer_Dao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Timer_POJO doInBackground(final Integer... params) {
            return mAsyncTaskDao.getTimerById(params[0]);
        }
    }


    //Update by id
    public void update(Timer_POJO updatedAlarm){
        new TimerRepository.updateAsyncTask(mTimerDao).execute(updatedAlarm);
    }

    private static class updateAsyncTask extends AsyncTask<Timer_POJO, Void, Void> {

        private Timer_Dao mAsyncTaskDao;

        updateAsyncTask(Timer_Dao dao) {
            mAsyncTaskDao = dao;
        }

        @Override
        protected Void doInBackground(final Timer_POJO... params) {
            mAsyncTaskDao.update(params[0]);
            return null;
        }
    }

}
