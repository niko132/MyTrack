package de.mytrack.mytrackapp.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

@Entity
public class Area {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "color")
    public int color;

    @Ignore
    public List<AreaPoint> points;

    public Area() {

    }

    @Ignore
    public Area(String name, int color, List<AreaPoint> points) {
        this.name = name;
        this.color = color;
        this.points = points;
    }

    public boolean pointsEqual(@NonNull Area rhs) {
        if (points == null && rhs.points == null)
            return true;

        if (points == null || rhs.points == null)
            return false;

        if (points.size() != rhs.points.size())
            return false;

        for (int i = 0; i < points.size(); i++) {
            if (!points.get(i).equals(rhs.points.get(i)))
                return false;
        }

        return true;
    }

}