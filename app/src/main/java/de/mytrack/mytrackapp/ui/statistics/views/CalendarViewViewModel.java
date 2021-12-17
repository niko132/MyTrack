package de.mytrack.mytrackapp.ui.statistics.views;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.spheric_geometry.PointOnSphere;
import de.mytrack.mytrackapp.spheric_geometry.PolygonOnEarth;

public class CalendarViewViewModel extends ViewModel {

    private final LiveData<List<List<VisitedArea>>> mDayData;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public CalendarViewViewModel() {
        // today
        Calendar begin = new GregorianCalendar();

        // reset hour, minutes, seconds and millis
        begin.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        setCalendarMidnight(begin);

        Calendar end = (Calendar) begin.clone();

        // next day
        end.add(Calendar.WEEK_OF_YEAR, 1);


        // get the locations and the areas as LiveData
        LiveData<List<TimeLocation>> locationsData = mDatabase.locationDao().getAllBetween(begin.getTime().getTime(), end.getTime().getTime());
        LiveData<List<Area>> areasData = mDatabase.areaDao().getAllAreasWithPoints();

        // define the observable data as a mediator so it gets updated either when
        // the locations change or when the area definitions change
        MediatorLiveData<List<List<VisitedArea>>> dayData = new MediatorLiveData<>();
        dayData.addSource(locationsData, timeLocations -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<List<VisitedArea>> tmp = getDailyVisitedAreas(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    dayData.setValue(tmp);
            }
        });
        dayData.addSource(areasData, areas -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<List<VisitedArea>> tmp = getDailyVisitedAreas(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    dayData.setValue(tmp);
            }
        });
        mDayData = dayData;
    }

    @NonNull
    private List<List<VisitedArea>> getDailyVisitedAreas(@NonNull List<TimeLocation> locations, @NonNull List<Area> areas) {
        PolygonOnEarth[] polygons = getPolygonsFromAreas(areas);
        List<VisitedArea> visitedAreas = extractVisitedAreas(locations, areas, polygons);

        return splitVisitedAreasAtDays(visitedAreas);
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

    /**
     * Converts a list of VisitedAreas into a list of lists of VisitedAreas so that every day
     * has its own list of VisitedAreas.
     * A VisitedArea that spans across multiple days gets split up into multiple VisitedAreas
     * and is added to the specific lists of every day.
     * The outer returned list always has a size of 7 and the inner lists are never null but
     * the may be empty.
     */
    @NonNull
    private List<List<VisitedArea>> splitVisitedAreasAtDays(@NonNull List<VisitedArea> visitedAreas) {
        // initialize data empty lists for every day
        List<List<VisitedArea>> daysData = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            daysData.add(new ArrayList<>());
        }

        for (VisitedArea visitedArea : visitedAreas) {
            Calendar begin = Calendar.getInstance();
            begin.setTimeInMillis(visitedArea.mEnterMs);

            Calendar nextDayMidnight = (Calendar) begin.clone();
            setCalendarMidnight(nextDayMidnight);
            nextDayMidnight.add(Calendar.DAY_OF_YEAR, 1);

            int beginDay = (begin.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
            long currentMs = visitedArea.mEnterMs;
            long remainingMs = visitedArea.mDurationMs;

            // iterate over every day from the beginning as long as there is time left
            for (int i = beginDay; i < 7 && remainingMs > 0; i++) {
                // get the remaining milliseconds of the current day from the currentMs timestamp
                long remainingDayMs = nextDayMidnight.getTimeInMillis() - currentMs;
                // take at most the whole day or the remaining milliseconds
                long duration = Math.min(remainingDayMs, remainingMs);

                // create a new copy for the day and adjust the timestamps
                VisitedArea copy = new VisitedArea();
                copy.mArea = visitedArea.mArea;
                copy.mEnterMs = currentMs;
                copy.mDurationMs = duration;
                daysData.get(i).add(copy);

                // advance every variable
                currentMs = nextDayMidnight.getTimeInMillis();
                remainingMs -= duration;
                nextDayMidnight.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        return daysData;
    }

    private void setCalendarMidnight(@NonNull Calendar calendar) {
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
    }

    public LiveData<List<List<VisitedArea>>> getDayData() {
        return mDayData;
    }

    static class VisitedArea {

        public Area mArea;
        public long mEnterMs;
        public long mDurationMs;

    }

}
