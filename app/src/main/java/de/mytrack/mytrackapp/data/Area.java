package de.mytrack.mytrackapp.data;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.List;
import java.util.Objects;

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

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Area area = (Area) o;
        return id == area.id && color == area.color && Objects.equals(name, area.name) && pointsEqual(area);
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