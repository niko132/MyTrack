package de.mytrack.mytrackapp.ui.statistics;

import android.os.Handler;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class StatisticsViewModel extends ViewModel {

    private MutableLiveData<String> mText;
    private int counter = 1;

    public StatisticsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is statistics fragment");

        final Handler handler = new Handler();

        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mText.setValue(counter + "");
                counter++;
                handler.postDelayed(this, 100);
            }
        }, 100);
    }

    public LiveData<String> getText() {
        return mText;
    }
}