package com.nicolai.alarm_clock.pojos;

import android.content.Context;

import com.nicolai.alarm_clock.util.SoundUtil;

import androidx.annotation.Nullable;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "timer_table")
public class Timer_POJO {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private long duration;
    private String name;
    private String sound;
    private int volume;

    private State state;
    private long millisLeft;
    private long ends;

    public Timer_POJO(int id, long duration, String name, String sound, int volume, State state, long millisLeft, long ends) {
        this.id = id;
        this.duration = duration;
        this.name = name;
        this.sound = sound;
        this.volume = volume;
        this.state = state;
        this.millisLeft = millisLeft;
        this.ends = ends;
    }

    @Ignore
    //For insert -> no id
    public Timer_POJO(long duration, String name, String sound, int volume, State state, long millisLeft, long ends) {
        this.duration = duration;
        this.name = name;
        this.sound = sound;
        this.volume = volume;
        this.state = state;
        this.millisLeft = millisLeft;
        this.ends = ends;
    }

    @Ignore
    //For insert -> no id
    public Timer_POJO() {
        id = -10;
    }

    @Ignore
    //For insert -> no id
    public Timer_POJO(Context context) {
        duration = 600000;
        name = "";
        sound = SoundUtil.defaultUri(context, false);
        volume = SoundUtil.getDefaultVolume(context);
        state = new State();
        millisLeft = duration;
        ends = 0;
    }

    public int getId() {
        return id;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long duration) {
        this.duration = duration;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSound() {
        return sound;
    }

    public void setSound(String sound) {
        this.sound = sound;
    }

    public int getVolume() {
        return volume;
    }

    public void setVolume(int volume) {
        this.volume = volume;
    }

    public State getState() {
        return state;
    }

    public void setState(State running) {
        this.state = running;
    }

    public long getMillisLeft() {
        return millisLeft;
    }

    public void updateMillisLeft(int timePassed) {
        this.millisLeft -= timePassed;
    }
    public void setMillisLeft(long millisLeft){
        this.millisLeft = millisLeft;
    }

    public long getEnds() {
        return ends;
    }

    public void setEnds(long ends) {
        this.ends = ends;
    }

    //Reset after gone off and stopped
    public void reset(){
        millisLeft = duration;
        state.setIdle();
        ends = 0;
    }

    //Equals method for RecyclerView ListAdapter
    @Override
    public boolean equals(@Nullable Object obj) {
        if(Timer_POJO.class.isInstance(obj)){
            Timer_POJO timer = (Timer_POJO) obj;

            if (timer.getId() == -10) return true;

            if(!this.name.equals(timer.getName())){
                return false;
            }
            if(this.ends != timer.ends){
                return false;
            }
            if(this.duration != timer.duration){
                return false;
            }
            if(StateConverter.fromState(this.state) != StateConverter.fromState(timer.state)){
                return false;
            }

            return this.millisLeft == timer.millisLeft;
        }

        return false;
    }
}
