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
import de.mytrack.mytrackapp.Utils;
import de.mytrack.mytrackapp.Utils.VisitedArea;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.TimeLocation;

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
        List<VisitedArea> visitedAreas = Utils.getVisitedAreas(locations, areas);
        return extractVisitedAreaDetails(visitedAreas);
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

    static class VisitedAreaDetail {
        public Area mArea;
        public long mLastEnterMs;
        public long mLastExitMs;
        public long mAverageDurationMs;
    }

}
