package de.mytrack.mytrackapp.ui.activities;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.CustomActivity;

public class ActivitiesViewModel extends ViewModel {

    private final LiveData<List<CustomActivity>> mCustomActivities;

    private final AppDatabase mDatabase = MyApplication.appContainer.database;

    public ActivitiesViewModel() {
        mCustomActivities = mDatabase.customActivityDao().getAll();
    }

    public LiveData<List<CustomActivity>> getCustomActivities() {
        return mCustomActivities;
    }

    public void onAddActivity(String name, int color) {
        AsyncTask.execute(() -> {
            CustomActivity newActivity = new CustomActivity(name, color);
            mDatabase.customActivityDao().insertAll(newActivity);
        });
    }

    public void onActivityPlusClicked(@NonNull CustomActivity activity) {
        activity.totalClicks++;
        activity.lastClickMs = System.currentTimeMillis();

        AsyncTask.execute(() -> mDatabase.customActivityDao().insertAll(activity));
    }

    public void onActivityDeleteClicked(CustomActivity activity) {
        AsyncTask.execute(() -> mDatabase.customActivityDao().delete(activity));
    }
}