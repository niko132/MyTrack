package de.mytrack.mytrackapp.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity
public class CustomActivity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "color")
    public int color;

    @ColumnInfo(name = "total_clicks")
    public long totalClicks;

    @ColumnInfo(name = "last_click_ms")
    public long lastClickMs;

    public CustomActivity() {

    }

    @Ignore
    public CustomActivity(String name, int color) {
        this.name = name;
        this.color = color;
        this.totalClicks = 0;
        this.lastClickMs = System.currentTimeMillis();
    }

}
