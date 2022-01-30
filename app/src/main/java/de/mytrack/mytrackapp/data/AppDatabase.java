package de.mytrack.mytrackapp.data;

import android.content.Context;

import androidx.room.AutoMigration;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(
        version = 5,
        entities = {TimeLocation.class, Area.class, AreaPoint.class, CustomActivity.class, LocationNetworks.class, BatteryDelayTime.class},
        autoMigrations = {
                @AutoMigration(from = 3, to = 4),
                @AutoMigration(from = 4, to = 5),
        }
        )
public abstract class AppDatabase extends RoomDatabase {
    private static final String DB_NAME = "mytrack_db";
    private static AppDatabase instance;

    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(context.getApplicationContext(), AppDatabase.class,
                    DB_NAME).build();
        }

        return instance;
    }

    public abstract LocationDao locationDao();

    public abstract AreaDao areaDao();

    public abstract CustomActivityDao customActivityDao();

    public abstract LocationNetworksDao locationNetworksDao();

    public abstract SettingsDao settingsDao();
}
