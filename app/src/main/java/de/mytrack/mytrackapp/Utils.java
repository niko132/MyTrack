package de.mytrack.mytrackapp;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;
import java.util.List;

import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.spheric_geometry.PointOnSphere;
import de.mytrack.mytrackapp.spheric_geometry.PolygonOnEarth;

public class Utils {

    /**
     * Tries to find an area that surrounds the given location.
     * @param latLng the location in the area
     * @return an observable Task that returns the found area or null
     */
    @NonNull
    public static Task<Area> getAreaFromLocation(@NonNull LatLng latLng) {
        TaskCompletionSource<Area> task = new TaskCompletionSource<>();

        LiveData<List<Area>> areasData = MyApplication.appContainer.database.areaDao().getAllAreasWithPoints();
        areasData.observeForever(new Observer<>() {
            @Override
            public void onChanged(List<Area> areas) {
                areasData.removeObserver(this);

                Area result = getAreaFromLocation(latLng, areas);
                task.setResult(result);
            }
        });

        return task.getTask();
    }

    /**
     * Tries to find an area that surrounds the given location.
     * The areas are passed as a parameter and don't need to be loaded.
     * @param latLng the location in the area
     * @param areas the areas to choose from
     * @return the found area or null
     */
    @Nullable
    public static Area getAreaFromLocation(@NonNull LatLng latLng, @NonNull List<Area> areas) {
        PointOnSphere locationOnSphere = new PointOnSphere(latLng.latitude, latLng.longitude);
        Area result = null;

        for (Area area : areas) {
            PolygonOnEarth polygon = PolygonOnEarth.from(area);
            if (polygon != null && polygon.includes(locationOnSphere)) {
                result = area;
                break;
            }
        }

        return result;
    }

    /**
     * Creates a list of VisitedAreas from the locations and area definitions.
     * It iterates over every location and connects locations in the same area to a single
     * VisitedArea object. The time of entry and the duration in the area are calculated from
     * the times of the locations in the specific area.
     * The list of locations is expected to be in reversed order (youngest first, oldest last).
     */
    @NonNull
    public static List<VisitedArea> getVisitedAreas(@NonNull List<TimeLocation> locations,
                                                    @NonNull List<Area> areas) {
        // initialize working variables
        List<VisitedArea> visitedAreas = new ArrayList<>();
        VisitedArea currentArea = null;

        // iterate over every location
        // we expect the locations to be in reversed order (youngest first)
        // therefore we also iterate in reversed order to get the oldest location first
        for (int i = locations.size() - 1; i >= 0; i--) {
            TimeLocation timeLocation = locations.get(i);
            LatLng latLng = new LatLng(timeLocation.latitude, timeLocation.longitude);
            Area area = Utils.getAreaFromLocation(latLng, areas);

            if (currentArea != null && currentArea.mArea.equals(area)) {
                currentArea.mDurationMs = timeLocation.time - currentArea.mEnterMs;
            } else if (area != null) {
                currentArea = new VisitedArea();
                currentArea.mArea = area;
                currentArea.mEnterMs = timeLocation.time;
                visitedAreas.add(currentArea);
            } else {
                currentArea = null;
            }
        }

        return visitedAreas;
    }

    @NonNull
    public static Task<Area> getAreaFromLastLocation() {
        TaskCompletionSource<Area> task = new TaskCompletionSource<>();

        LiveData<TimeLocation> locationData = MyApplication.appContainer.database.locationDao().getMostRecent();
        locationData.observeForever(new Observer<>() {
            @Override
            public void onChanged(TimeLocation timeLocation) {
                locationData.removeObserver(this);

                if (timeLocation != null) {
                    getAreaFromLocation(new LatLng(timeLocation.latitude, timeLocation.longitude))
                            .addOnCompleteListener(areaTask -> task.setResult(areaTask.getResult()));
                } else {
                    task.setResult(null);
                }
            }
        });

        return task.getTask();
    }

    /**
     * A data class that is commonly used in the statistics package.
     */
    public static class VisitedArea {

        public Area mArea;
        public long mEnterMs;
        public long mDurationMs;

    }

}
