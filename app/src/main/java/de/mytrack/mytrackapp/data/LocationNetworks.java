package de.mytrack.mytrackapp.data;

import android.text.TextUtils;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.google.android.gms.maps.model.LatLng;

import java.util.Arrays;
import java.util.List;

@Entity
public class LocationNetworks {

    @PrimaryKey
    @ColumnInfo(name = "area_id")
    public long areaId;

    @ColumnInfo(name = "ssids")
    public String ssids;

    public LocationNetworks() {

    }

    @Ignore
    public LocationNetworks(Area area, List<String> ssids) {
        this.areaId = area.id;
        setSSIDs(ssids);
    }

    public List<String> getSSIDs() {
        return Arrays.asList(ssids.split(","));
    }

    public void setSSIDs(List<String> ssids) {
        this.ssids = TextUtils.join(",", ssids);
    }

}
