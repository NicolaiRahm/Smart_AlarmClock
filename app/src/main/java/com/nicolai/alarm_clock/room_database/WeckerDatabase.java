package com.nicolai.alarm_clock.room_database;

import android.content.Context;
import android.os.Build;

import com.nicolai.alarm_clock.pojos.WeckerPOJO;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(entities = {WeckerPOJO.class}, version = 4, exportSchema = false)
public abstract class WeckerDatabase extends RoomDatabase {

    public static final String DATABASE_NAME = "calming_alarm";
    public abstract WeckerDao weckerDao();

    private static volatile WeckerDatabase INSTANCE;

    //Singelton pattern --> sicherstellen, dass nur ein Objekt der Datenbank erstellt wird
    public static WeckerDatabase getInstance(Context context){
        if(INSTANCE == null){
            synchronized (WeckerDatabase.class){
                //Use different context for direct boot
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    INSTANCE = Room.databaseBuilder(context.createDeviceProtectedStorageContext(), WeckerDatabase.class, DATABASE_NAME)
                            //TODO nur zum testen sonst async
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_3_4)
                            .build();
                } else {
                    INSTANCE = Room.databaseBuilder(context, WeckerDatabase.class, DATABASE_NAME)
                            //TODO nur zum testen sonst async
                            .fallbackToDestructiveMigration()
                            .allowMainThreadQueries()
                            .addMigrations(MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }

    private static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            // Since we didn't alter the table, there's nothing else to do here.
        }
    };
}
