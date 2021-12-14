package de.mytrack.mytrackapp.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {TimeLocation.class, Area.class, AreaPoint.class}, version = 3)
public abstract class AppDatabase extends RoomDatabase {
    private static final String DB_NAME = "mytrack_db";
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class,
                    DB_NAME).fallbackToDestructiveMigration().build();

            // TODO: remove destructive migration and implement manual migration
        }

        return instance;
    }

    public abstract LocationDao locationDao();

    public abstract AreaDao areaDao();
}
