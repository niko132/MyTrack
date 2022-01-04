package de.mytrack.mytrackapp.ui.statistics.views;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.spheric_geometry.PointOnSphere;
import de.mytrack.mytrackapp.spheric_geometry.PolygonOnEarth;

public class AreasViewViewModel extends ViewModel {

    private final LiveData<List<VisitedAreaDetail>> mAreaDetailData;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public AreasViewViewModel() {
        // TODO: maybe adjust the time to be considered (whole data, last year/month/week)
        // today
        Calendar end = new GregorianCalendar();
        Calendar begin = (Calendar) end.clone();

        // whole last week
        begin.add(Calendar.WEEK_OF_YEAR, -1);


        // get the locations and the areas as LiveData
        LiveData<List<TimeLocation>> locationsData = mDatabase.locationDao().getAllBetween(begin.getTime().getTime(), end.getTime().getTime());
        LiveData<List<Area>> areasData = mDatabase.areaDao().getAllAreasWithPoints();

        // define the observable data as a mediator so it gets updated either when
        // the locations change or when the area definitions change
        MediatorLiveData<List<VisitedAreaDetail>> detailData = new MediatorLiveData<>();
        detailData.addSource(locationsData, timeLocations -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<VisitedAreaDetail> tmp = getVisitedAreaDetails(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    detailData.setValue(tmp);
            }
        });
        detailData.addSource(areasData, areas -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<VisitedAreaDetail> tmp = getVisitedAreaDetails(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    detailData.setValue(tmp);
            }
        });
        mAreaDetailData = detailData;
    }

    @NonNull
    private List<VisitedAreaDetail> getVisitedAreaDetails(@NonNull List<TimeLocation> locations, @NonNull List<Area> areas) {
        PolygonOnEarth[] polygons = getPolygonsFromAreas(areas);
        List<VisitedArea> visitedAreas = extractVisitedAreas(locations, areas, polygons);

        return extractVisitedAreaDetails(visitedAreas);
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
    private List<VisitedAreaDetail> extractVisitedAreaDetails(@NonNull List<VisitedArea> visitedAreas) {
        Map<Area, List<VisitedArea>> allVisitedAreas = new HashMap<>();

        for (VisitedArea visitedArea : visitedAreas) {
            List<VisitedArea> allList = allVisitedAreas.get(visitedArea.mArea);
            if (allList == null) {
                allList = new ArrayList<>();
                allVisitedAreas.put(visitedArea.mArea, allList);
            }

            allList.add(visitedArea);
        }

        List<VisitedAreaDetail> visitedAreaDetails = new ArrayList<>();

        for (Map.Entry<Area, List<VisitedArea>> entry : allVisitedAreas.entrySet()) {
            VisitedAreaDetail detail = new VisitedAreaDetail();
            detail.mArea = entry.getKey();

            long lastEnterMs = 0;
            long lastExitMs = 0;
            long averageDurationMs = 0;

            for (VisitedArea visitedArea : entry.getValue()) {
                long enterMs = visitedArea.mEnterMs;
                long exitMs = visitedArea.mEnterMs + visitedArea.mDurationMs;
                long durationMs = visitedArea.mDurationMs;

                if (enterMs > lastEnterMs)
                    lastEnterMs = enterMs;

                if (exitMs > lastExitMs)
                    lastExitMs = exitMs;

                averageDurationMs += durationMs;
            }

            averageDurationMs /= entry.getValue().size();

            detail.mLastEnterMs = lastEnterMs;
            detail.mLastExitMs = lastExitMs;
            detail.mAverageDurationMs = averageDurationMs;

            visitedAreaDetails.add(detail);
        }

        // TODO: sort the list by some value (e.g. recent first)
        // or show every area in the list

        return visitedAreaDetails;
    }

    public LiveData<List<VisitedAreaDetail>> getAreaDetailData() {
        return mAreaDetailData;
    }

    static class VisitedArea {

        public Area mArea;
        public long mEnterMs;
        public long mDurationMs;

    }

    static class VisitedAreaDetail {
        public Area mArea;
        public long mLastEnterMs;
        public long mLastExitMs;
        public long mAverageDurationMs;
    }

}
