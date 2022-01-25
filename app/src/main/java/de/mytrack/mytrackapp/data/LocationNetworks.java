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

    @PrimaryKey(autoGenerate = true)
    public long id;

    @ColumnInfo(name = "latitude")
    public double latitude;

    @ColumnInfo(name = "longitude")
    public double longitude;

    @ColumnInfo(name = "ssids")
    public String ssids;

    public LocationNetworks() {

    }

    @Ignore
    public LocationNetworks(LatLng latLng, List<String> ssids) {
        this.latitude = latLng.latitude;
        this.longitude = latLng.longitude;

        setSSIDs(ssids);
    }

    public List<String> getSSIDs() {
        return Arrays.asList(ssids.split(","));
    }

    public void setSSIDs(List<String> ssids) {
        this.ssids = TextUtils.join(",", ssids);
    }

}
