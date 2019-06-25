package com.nicolai.alarm_clock.pojos;

import androidx.room.TypeConverter;

public class StateConverter {

    @TypeConverter
    public static State toState(int state){

        switch (state){
            case 0: return new State(true, false, false);
            case 1: return new State(false, true, false);
            case 2: return new State(false, false, true);
            default: return new State();
        }
    }

    @TypeConverter
    public static int fromState(State state){
        if (state.isRunning()) return 0;
        if (state.isPaused()) return 1;
        if (state.isFinished()) return 2;
        return 3;
    }

}
