package de.mytrack.mytrackapp.ui.statistics.views;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.ViewModel;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.TimeLocation;

public class CalendarViewViewModel extends ViewModel {

    private final LiveData<List<VisitedArea>> mVisitedAreas;
    private final LiveData<List<List<VisitedArea>>> mDayData;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public CalendarViewViewModel() {
        // today
        Calendar begin = new GregorianCalendar();

        // reset hour, minutes, seconds and millis
        begin.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        begin.set(Calendar.HOUR_OF_DAY, 0);
        begin.set(Calendar.MINUTE, 0);
        begin.set(Calendar.SECOND, 0);
        begin.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) begin.clone();

        // next day
        end.add(Calendar.WEEK_OF_YEAR, 1);


        MediatorLiveData<List<VisitedArea>> visitedAreas = new MediatorLiveData<>();
        /*
        LiveData<List<TimeLocation>> locationsData = mDatabase.locationDao().getAllBetween(begin.getTime().getTime(), end.getTime().getTime());
        LiveData<List<Area>> areasData = mDatabase.areaDao().getAllAreasWithPoints();
        visitedAreas.addSource(locationsData, timeLocations -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<VisitedArea> tmp = doMagic(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    visitedAreas.setValue(tmp);
            }
        });
        visitedAreas.addSource(areasData, areas -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<VisitedArea> tmp = doMagic(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    visitedAreas.setValue(tmp);
            }
        });
        */
        mVisitedAreas = visitedAreas;




        MediatorLiveData<List<List<VisitedArea>>> dayData = new MediatorLiveData<>();
        LiveData<List<TimeLocation>> locationsData = mDatabase.locationDao().getAllBetween(begin.getTime().getTime(), end.getTime().getTime());
        LiveData<List<Area>> areasData = mDatabase.areaDao().getAllAreasWithPoints();
        dayData.addSource(locationsData, timeLocations -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<List<VisitedArea>> tmp = doMagic1(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    dayData.setValue(tmp);
            }
        });
        dayData.addSource(areasData, areas -> {
            if (locationsData.getValue() != null && areasData.getValue() != null) {
                List<List<VisitedArea>> tmp = doMagic1(locationsData.getValue(), areasData.getValue());
                if (!tmp.isEmpty())
                    dayData.setValue(tmp);
            }
        });
        mDayData = dayData;
    }

    @NonNull
    private List<VisitedArea> doMagic(@NonNull List<TimeLocation> locations, @NonNull List<Area> areas) {
        LatLngBounds[] areaBounds = new LatLngBounds[areas.size()];

        for (int i = 0; i < areas.size(); i++) {
            Area area = areas.get(i);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (AreaPoint point : area.points) {
                builder.include(new LatLng(point.latitude, point.longitude));
            }

            areaBounds[i] = builder.build();
        }

        List<VisitedArea> visitedAreas = new ArrayList<>();
        VisitedArea currentArea = null;
        int currentBoundsIndex = -1;

        for (int i = locations.size() - 1; i >= 0; i--) {
            TimeLocation timeLocation = locations.get(i);
            LatLng location = new LatLng(timeLocation.latitude, timeLocation.longitude);

            if (currentArea != null && areaBounds[currentBoundsIndex].contains(location)) {
                currentArea.mDurationMs = timeLocation.time - currentArea.mEnterMs;
            } else {
                for (int j = 0; j < areaBounds.length; j++) {
                    if (areaBounds[j].contains(location)) {
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
    private List<List<VisitedArea>> doMagic1(@NonNull List<TimeLocation> locations, @NonNull List<Area> areas) {
        LatLngBounds[] areaBounds = new LatLngBounds[areas.size()];

        for (int i = 0; i < areas.size(); i++) {
            Area area = areas.get(i);
            LatLngBounds.Builder builder = new LatLngBounds.Builder();

            for (AreaPoint point : area.points) {
                builder.include(new LatLng(point.latitude, point.longitude));
            }

            areaBounds[i] = builder.build();
        }

        List<VisitedArea> visitedAreas = new ArrayList<>();
        VisitedArea currentArea = null;
        int currentBoundsIndex = -1;

        for (int i = locations.size() - 1; i >= 0; i--) {
            TimeLocation timeLocation = locations.get(i);
            LatLng location = new LatLng(timeLocation.latitude, timeLocation.longitude);

            if (currentArea != null && areaBounds[currentBoundsIndex].contains(location)) {
                currentArea.mDurationMs = timeLocation.time - currentArea.mEnterMs;
            } else {
                for (int j = 0; j < areaBounds.length; j++) {
                    if (areaBounds[j].contains(location)) {
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


        List<List<VisitedArea>> daysData = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            List<VisitedArea> dayData = new ArrayList<>();
            daysData.add(dayData);

            for (VisitedArea visitedArea : visitedAreas) {
                Calendar begin = Calendar.getInstance();
                begin.setTimeInMillis(visitedArea.mEnterMs);

                Calendar end = Calendar.getInstance();
                end.setTimeInMillis(visitedArea.mEnterMs + visitedArea.mDurationMs);


                int beginDay = (begin.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;
                int endDay = (end.get(Calendar.DAY_OF_WEEK) - Calendar.MONDAY + 7) % 7;

                if (beginDay == i && endDay == i) {
                    // just add
                    dayData.add(visitedArea);
                } else if (beginDay == i) {
                    // get end of the day
                    Calendar endOfDay = (Calendar) begin.clone();
                    endOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    endOfDay.set(Calendar.MINUTE, 0);
                    endOfDay.set(Calendar.SECOND, 0);
                    endOfDay.set(Calendar.MILLISECOND, 0);
                    endOfDay.add(Calendar.DAY_OF_YEAR, 1);

                    long endOfDayMs = endOfDay.getTimeInMillis();

                    VisitedArea copy = new VisitedArea();
                    copy.mArea = visitedArea.mArea;
                    copy.mEnterMs = visitedArea.mEnterMs;
                    copy.mDurationMs = endOfDayMs - copy.mEnterMs;

                    dayData.add(copy);
                } else if (endDay == i) {
                    // get end of the day
                    Calendar beginOfDay = (Calendar) end.clone();
                    beginOfDay.set(Calendar.HOUR_OF_DAY, 0);
                    beginOfDay.set(Calendar.MINUTE, 0);
                    beginOfDay.set(Calendar.SECOND, 0);
                    beginOfDay.set(Calendar.MILLISECOND, 0);

                    long beginOfDayMs = beginOfDay.getTimeInMillis();

                    VisitedArea copy = new VisitedArea();
                    copy.mArea = visitedArea.mArea;
                    copy.mEnterMs = beginOfDayMs;
                    copy.mDurationMs = visitedArea.mEnterMs + visitedArea.mDurationMs - beginOfDayMs;

                    dayData.add(copy);
                }
            }
        }

        return daysData;
    }

    public LiveData<List<VisitedArea>> getVisitedAreas() {
        return mVisitedAreas;
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
