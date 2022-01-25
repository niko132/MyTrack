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
import de.mytrack.mytrackapp.Utils;
import de.mytrack.mytrackapp.Utils.VisitedArea;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.TimeLocation;

public class CalendarViewViewModel extends ViewModel {

    private final LiveData<List<List<VisitedArea>>> mDayData;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public CalendarViewViewModel() {
        // today
        Calendar begin = new GregorianCalendar();

        // reset day, hour, minutes, seconds and millis
        begin.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        setCalendarMidnight(begin);

        Calendar end = (Calendar) begin.clone();

        // end of the current week
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
        List<VisitedArea> visitedAreas = Utils.getVisitedAreas(locations, areas);
        return splitVisitedAreasAtDays(visitedAreas);
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

}
