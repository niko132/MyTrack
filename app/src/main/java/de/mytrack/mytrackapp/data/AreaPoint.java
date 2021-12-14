package de.mytrack.mytrackapp.data;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import java.util.Objects;

@Entity
public class AreaPoint {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "area_id")
    public long areaId;

    @ColumnInfo(name = "idx")
    public int index;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    public AreaPoint() {

    }

    @Ignore
    public AreaPoint(double latitude, double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AreaPoint point = (AreaPoint) o;
        return Double.compare(point.latitude, latitude) == 0 &&
                Double.compare(point.longitude, longitude) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, areaId, latitude, longitude);
    }
}