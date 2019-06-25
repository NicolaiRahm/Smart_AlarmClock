package com.nicolai.alarm_clock.room_database;

import com.nicolai.alarm_clock.pojos.Timer_POJO;

import java.util.List;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

@Dao
public interface Timer_Dao {

    @Insert
    public long insert(Timer_POJO timer);

    @Update
    public void update(Timer_POJO timer);

    @Query("SELECT * FROM timer_table")
    public LiveData<List<Timer_POJO>> getAllTimers();
    @Query("SELECT * FROM timer_table ORDER BY id LIMIT 1")
    public Timer_POJO latestTimer();
    @Query("SELECT * FROM timer_table")
    public List<Timer_POJO> getBootList();
    @Query("SELECT * FROM timer_table WHERE id = :id")
    public Timer_POJO getTimerById(int id);

    @Delete
    public void delete(Timer_POJO alarm);
    @Query("DELETE FROM timer_table WHERE id = :id")
    public void deleteById(int id);
}