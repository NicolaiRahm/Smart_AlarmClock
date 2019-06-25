package com.nicolai.alarm_clock.room_database;

import android.content.Context;
import android.os.Build;

import com.nicolai.alarm_clock.pojos.StateConverter;
import com.nicolai.alarm_clock.pojos.Timer_POJO;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;

@Database(entities = {Timer_POJO.class}, version = 1, exportSchema = false)
@TypeConverters({StateConverter.class})
public abstract class TimerRoomDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "timer_database";
    public abstract Timer_Dao timerDao();

    private static volatile TimerRoomDatabase INSTANCE;

    public static TimerRoomDatabase getDatabase(final Context context){
        if(INSTANCE == null){
            synchronized (TimerRoomDatabase.class) {
                if (INSTANCE == null) {
                    //Use different context for direct boot
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                        INSTANCE = Room.databaseBuilder(context.createDeviceProtectedStorageContext(), TimerRoomDatabase.class, DATABASE_NAME)
                                .fallbackToDestructiveMigration()
                                .build();
                    } else {
                        INSTANCE = Room.databaseBuilder(context, TimerRoomDatabase.class, DATABASE_NAME)
                                .fallbackToDestructiveMigration()
                                .build();
                    }

                }
            }
        }

        return INSTANCE;
    }
}
