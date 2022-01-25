package de.mytrack.mytrackapp.ui.areas;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.List;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;

public class AreasViewModel extends ViewModel {

    // store the polygons in a LiveData object to get updates
    private final LiveData<List<Area>> mAreas;
    private final MutableLiveData<Area> mFocusedArea;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public AreasViewModel() {
        mFocusedArea = new MutableLiveData<>();
        mAreas = mDatabase.areaDao().getAllAreasWithPoints();
    }

    public void onAddArea(String name, int color, @NonNull LatLngBounds bounds) {
        List<AreaPoint> points = new ArrayList<>();

        AreaPoint point1 = new AreaPoint(bounds.southwest.latitude, bounds.southwest.longitude);
        AreaPoint point2 = new AreaPoint(bounds.southwest.latitude, bounds.northeast.longitude);
        AreaPoint point3 = new AreaPoint(bounds.northeast.latitude, bounds.northeast.longitude);
        AreaPoint point4 = new AreaPoint(bounds.northeast.latitude, bounds.southwest.longitude);

        points.add(point1);
        points.add(point2);
        points.add(point3);
        points.add(point4);

        Area area = new Area(name, color, points);
        AsyncTask.execute(() -> mDatabase.areaDao().insertAreaWithPoints(area));
    }

    public void onDeleteArea(@NonNull Area area) {
        AsyncTask.execute(() -> mDatabase.areaDao().deleteAreaWithPoints(area));
    }

    public void onAreaChanged(Area area) {
        AsyncTask.execute(() -> mDatabase.areaDao().insertAreaWithPoints(area));
    }

    public void onFocus(@Nullable Area area) {
        mFocusedArea.setValue(area);
    }

    public LiveData<List<Area>> getAreas() {
        return mAreas;
    }

    public LiveData<Area> getFocusedArea() {
        return mFocusedArea;
    }
}