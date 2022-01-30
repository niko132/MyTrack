package de.mytrack.mytrackapp.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SettingsDao {

    @Query("SELECT delay_in_min FROM BatteryDelayTime WHERE battery_level = 50")
    int getNormalDelayTime();

    @Query("SELECT delay_in_min FROM BatteryDelayTime WHERE battery_level = 30")
    int getLowDelayTime();

    @Query("SELECT delay_in_min FROM BatteryDelayTime WHERE battery_level = 0")
    int getCriticalDelayTime();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(BatteryDelayTime... delayTimes);

    @Delete
    void delete(BatteryDelayTime delayTime);

}
