package de.mytrack.mytrackapp;

import android.app.Application;

public class MyApplication extends Application {

    public static AppContainer appContainer = null;

    @Override
    public void onCreate() {
        super.onCreate();
        appContainer = new AppContainer(getApplicationContext());
    }
}
