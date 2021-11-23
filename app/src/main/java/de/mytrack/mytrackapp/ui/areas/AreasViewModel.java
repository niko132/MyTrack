package de.mytrack.mytrackapp.ui.areas;

import android.util.Log;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AreasViewModel extends ViewModel {

    // create a helper class that stores information of a polygon
    // TODO: create a real class with database for this
    public static class Area {
        public int id;
        public String name;
        public List<LatLng> points;

        public Area(String name, @NonNull LatLng point) {
            this.id = new Random().nextInt();
            this.name = name;
            this.points = new ArrayList<>();

            this.points.add(new LatLng(point.latitude - 5, point.longitude - 5));
            this.points.add(new LatLng(point.latitude - 5, point.longitude + 5));
            this.points.add(new LatLng(point.latitude + 5, point.longitude + 5));
            this.points.add(new LatLng(point.latitude + 5, point.longitude - 5));
        }
    }

    // store the polygons in a LiveData object to get updates
    private final MutableLiveData<List<Area>> mAreas;

    public AreasViewModel() {
        mAreas = new MutableLiveData<>();
        mAreas.setValue(new ArrayList<>());
    }

    public void onAddArea(String name, LatLng point) {
        mAreas.getValue().add(new Area(name, point));
        // notify observers
        mAreas.setValue(mAreas.getValue());
    }

    public void onAreaChanged(Area area) {
        List<Area> areas = mAreas.getValue();
        if (areas == null)
            areas = new ArrayList<>();

        // search for area with same id and replace it
        boolean found = false;
        for (int i = 0; i < areas.size(); i++) {
            if (areas.get(i).id == area.id) {
                Log.d("main", "Area " + area.id + " at index " + i + " changed");
                areas.set(i, area);
                found = true;
            }
        }

        if (!found)
            areas.add(area);

        // notify observers
        mAreas.setValue(areas);
    }

    public LiveData<List<Area>> getAreas() {
        return mAreas;
    }
}