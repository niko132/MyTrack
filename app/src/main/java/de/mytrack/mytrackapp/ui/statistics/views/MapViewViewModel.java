package de.mytrack.mytrackapp.ui.statistics.views;

import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.spheric_geometry.PointOnSphere;
import de.mytrack.mytrackapp.spheric_geometry.PolygonOnEarth;

public class MapViewViewModel extends ViewModel {

    @Nullable
    private LiveData<List<TimeLocation>> mLocationsData;
    private final LiveData<List<Area>> mAreasData;
    private final MediatorLiveData<AreaConnections> mAreaConnectionsData;

    private final Calendar mCurrentDate;
    private final MutableLiveData<String> mCurrentDateText;
    private final MutableLiveData<Boolean> mNextDayButtonEnabled;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public MapViewViewModel() {
        mCurrentDateText = new MutableLiveData<>();
        mNextDayButtonEnabled = new MutableLiveData<>();

        mCurrentDate = new GregorianCalendar();
        setCalendarMidnight(mCurrentDate);

        mAreasData = mDatabase.areaDao().getAllAreasWithPoints();

        mAreaConnectionsData = new MediatorLiveData<>();
        mAreaConnectionsData.addSource(mAreasData, areas -> updateAreaConnectionsData());

        reloadData();
    }

    private void reloadData() {
        updateDateText();
        updateNextDayButtonEnabled();

        if (mLocationsData != null) {
            mAreaConnectionsData.removeSource(mLocationsData);
        }

        Calendar end = (Calendar) mCurrentDate.clone();
        end.add(Calendar.DAY_OF_YEAR, 1);
        mLocationsData = mDatabase.locationDao().getAllBetween(mCurrentDate.getTime().getTime(), end.getTime().getTime());
        mAreaConnectionsData.addSource(mLocationsData, timeLocations -> updateAreaConnectionsData());
    }

    private void updateAreaConnectionsData() {
        if (mLocationsData != null && mLocationsData.getValue() != null && mAreasData.getValue() != null) {
            AreaConnections tmp = getAreaConnections(mLocationsData.getValue(), mAreasData.getValue());
            mAreaConnectionsData.setValue(tmp);
        }
    }

    private void setCalendarMidnight(@NonNull Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    @NonNull
    private AreaConnections getAreaConnections(@NonNull List<TimeLocation> locations, @NonNull List<Area> areas) {
        PolygonOnEarth[] polygons = getPolygonsFromAreas(areas);
        List<VisitedArea> visitedAreas = extractVisitedAreas(locations, areas, polygons);

        return extractConnections(visitedAreas);
    }

    /**
     * Converts a list of areas into an array of PolygonOnEarth objects.
     * A polygon may be null if it is not convex.
     */
    @NonNull
    private PolygonOnEarth[] getPolygonsFromAreas(@NonNull List<Area> areas) {
        PolygonOnEarth[] polygons = new PolygonOnEarth[areas.size()];

        // store a PolygonOnEarth object for every area (or null if there is an exception)
        for (int i = 0; i < areas.size(); i++) {
            Area area = areas.get(i);
            PointOnSphere[] points = new PointOnSphere[area.points.size()];

            for (int j = 0; j < area.points.size(); j++) {
                AreaPoint point = area.points.get(j);
                points[j] = new PointOnSphere(point.latitude, point.longitude);
            }

            try {
                polygons[i] = new PolygonOnEarth(points);
            } catch (Exception e) {
                e.printStackTrace();
                polygons[i] = null;
            }
        }

        return polygons;
    }

    /**
     * Creates a list of VisitedAreas from the locations and area definitions.
     * It iterates over every location and connects locations in the same area to a single
     * VisitedArea object. The time of entry and the duration in the area are calculated from
     * the times of the locations in the specific area.
     * The list of locations is expected to be in reversed order (youngest first, oldest last).
     */
    @NonNull
    private List<VisitedArea> extractVisitedAreas(@NonNull List<TimeLocation> locations,
                                                  @NonNull List<Area> areas,
                                                  @NonNull PolygonOnEarth[] polygons) {
        // initialize working variables
        List<VisitedArea> visitedAreas = new ArrayList<>();
        VisitedArea currentArea = null;
        int currentBoundsIndex = -1;

        // iterate over every location
        // we expect the locations to be in reversed order (youngest first)
        // therefore we also iterate in reversed order to get the oldest location first
        for (int i = locations.size() - 1; i >= 0; i--) {
            TimeLocation timeLocation = locations.get(i);
            PointOnSphere point = new PointOnSphere(timeLocation.latitude, timeLocation.longitude);

            if (currentArea != null && polygons[currentBoundsIndex].includes(point)) {
                currentArea.mDurationMs = timeLocation.time - currentArea.mEnterMs;
            } else {
                // fixes problem of whitespace areas
                currentArea = null;
                currentBoundsIndex = -1;

                for (int j = 0; j < polygons.length; j++) {
                    if (polygons[j] != null && polygons[j].includes(point)) {
                        currentArea = new VisitedArea();
                        currentArea.mArea = areas.get(j);
                        currentArea.mEnterMs = timeLocation.time;
                        currentBoundsIndex = j;
                        visitedAreas.add(currentArea);
                        break;
                    }
                }
            }
        }

        return visitedAreas;
    }

    @NonNull
    private AreaConnections extractConnections(@NonNull List<VisitedArea> visitedAreas) {
        Set<Area> areaSet = new HashSet<>();
        for (MapViewViewModel.VisitedArea visitedArea : visitedAreas) {
            areaSet.add(visitedArea.mArea);
        }

        // TODO: put the test code in a unit test class
        /*
        visitedAreas = new ArrayList<>();

        Area a1 = new Area();
        a1.id = 1;

        Area a2 = new Area();
        a2.id = 2;

        Area a3 = new Area();
        a3.id = 3;


        VisitedArea va1 = new VisitedArea();
        va1.mArea = a1;

        VisitedArea va2 = new VisitedArea();
        va2.mArea = a2;

        VisitedArea va3 = new VisitedArea();
        va3.mArea = a1;

        VisitedArea va4 = new VisitedArea();
        va4.mArea = a3;

        VisitedArea va5 = new VisitedArea();
        va5.mArea = a2;

        VisitedArea va6 = new VisitedArea();
        va6.mArea = a1;


        visitedAreas.add(va1);
        visitedAreas.add(va2);
        visitedAreas.add(va3);
        visitedAreas.add(va4);
        visitedAreas.add(va5);
        visitedAreas.add(va6);
        */


        Set<Pair<Area, Area>> connectionSet = new HashSet<>();
        for (int i = 0; i < visitedAreas.size() - 1; i++) {
            Area area1 = visitedAreas.get(i).mArea;
            Area area2 = visitedAreas.get(i + 1).mArea;

            Area minArea = area1.id < area2.id ? area1 : area2;
            Area maxArea = area1.id < area2.id ? area2 : area1;

            Pair<Area, Area> pair = new Pair<>(minArea, maxArea);
            connectionSet.add(pair);
        }

        AreaConnections areaConnections = new AreaConnections();
        areaConnections.mAreas = areaSet;
        areaConnections.mConnections = connectionSet;

        return areaConnections;
    }

    public void prevDayClicked() {
        mCurrentDate.add(Calendar.DAY_OF_YEAR, -1);
        reloadData();
    }

    public void nextDayClicked() {
        mCurrentDate.add(Calendar.DAY_OF_YEAR, 1);
        reloadData();
    }

    public void updateDateText() {
        String dateText = DateUtils.getRelativeTimeSpanString(
                mCurrentDate.getTimeInMillis(),
                System.currentTimeMillis(),
                DateUtils.DAY_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_ALL).toString();
        mCurrentDateText.setValue(dateText);
    }

    public void updateNextDayButtonEnabled() {
        Calendar today = new GregorianCalendar();
        setCalendarMidnight(today);
        mNextDayButtonEnabled.setValue(mCurrentDate.before(today));
    }

    public LiveData<AreaConnections> getAreaConnectionsData() {
        return mAreaConnectionsData;
    }

    public LiveData<String> getCurrentDateText() {
        return mCurrentDateText;
    }

    public LiveData<Boolean> getNextDayButtonEnabled() {
        return mNextDayButtonEnabled;
    }

    static class VisitedArea {

        public Area mArea;
        public long mEnterMs;
        public long mDurationMs;

    }

    static class AreaConnections {

        public Set<Area> mAreas;
        public Set<Pair<Area, Area>> mConnections;

    }

}
