package de.mytrack.mytrackapp;

import android.content.Context;

import androidx.annotation.NonNull;

import de.mytrack.mytrackapp.data.AppDatabase;

public class AppContainer {

    public AppDatabase database;

    public AppContainer(@NonNull Context context) {
        database = AppDatabase.getInstance(context.getApplicationContext());
    }

}
