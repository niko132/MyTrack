package de.mytrack.mytrackapp.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class BatteryDelayTime {

    @PrimaryKey
    @ColumnInfo(name = "battery_level", defaultValue = "0")
    public int batteryLevel;

    @ColumnInfo(name = "delay_in_min", defaultValue = "10")
    public int delayInMin;

    public BatteryDelayTime() {

    }

    @NonNull
    @Ignore
    public static BatteryDelayTime normal(int delayInMin) {
        BatteryDelayTime delayTime = new BatteryDelayTime();
        delayTime.batteryLevel = 50;
        delayTime.delayInMin = delayInMin;
        return delayTime;
    }

    @NonNull
    @Ignore
    public static BatteryDelayTime low(int delayInMin) {
        BatteryDelayTime delayTime = new BatteryDelayTime();
        delayTime.batteryLevel = 30;
        delayTime.delayInMin = delayInMin;
        return delayTime;
    }

    @NonNull
    @Ignore
    public static BatteryDelayTime critical(int delayInMin) {
        BatteryDelayTime delayTime = new BatteryDelayTime();
        delayTime.batteryLevel = 0;
        delayTime.delayInMin = delayInMin;
        return delayTime;
    }

}
