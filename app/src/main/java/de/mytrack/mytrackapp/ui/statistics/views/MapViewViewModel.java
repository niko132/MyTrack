package de.mytrack.mytrackapp.ui.statistics.views;

import android.text.format.DateUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.Utils;
import de.mytrack.mytrackapp.Utils.VisitedArea;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.TimeLocation;

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
        List<VisitedArea> visitedAreas = Utils.getVisitedAreas(locations, areas);
        return extractConnections(visitedAreas);
    }

    @NonNull
    private AreaConnections extractConnections(@NonNull List<VisitedArea> visitedAreas) {
        Set<Area> areaSet = new HashSet<>();
        for (VisitedArea visitedArea : visitedAreas) {
            areaSet.add(visitedArea.mArea);
        }

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

    static class AreaConnections {

        public Set<Area> mAreas;
        public Set<Pair<Area, Area>> mConnections;

    }

}
