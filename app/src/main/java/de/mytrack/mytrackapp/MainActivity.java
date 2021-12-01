package de.mytrack.mytrackapp;

import android.Manifest;
import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.List;

import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                R.id.navigation_statistics, R.id.navigation_activities, R.id.navigation_areas)
                .build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(binding.navView, navController);
    }

    @Override
    protected void onStart() {
        super.onStart();

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        // ask the user for location permissions
        registerForActivityResult(new ActivityResultContracts.
                RequestMultiplePermissions(), result -> {
            boolean fineLocationGranted =
                    (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) &&
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) ?
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION) : false;

            boolean coarseLocationGranted =
                    (result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            result.get(Manifest.permission.ACCESS_COARSE_LOCATION) != null) ?
                            result.get(Manifest.permission.ACCESS_COARSE_LOCATION) : false;

            if (fineLocationGranted || coarseLocationGranted) {
                Log.d("main", "Location Granted");

                // start service only when we have the location permission
                startLocationService();
            } else {
                Log.d("main", "No Location Granted");
            }
        }).launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void startLocationService() {
        /*
        // create an intent for the service
        Intent locationServiceIntent = new Intent(this, LocationService.class);

        // start it as a ForegroundService for newer Android versions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }
        */

        // use new smart service
        SmartLocationService.startService(this);

        // print the whole database
        AppDatabase.getInstance(this).locationDao().getAll().observe(this, timeLocations -> {
            Log.d("main", "===================================================");
            for (TimeLocation timeLoc : timeLocations) {
                Log.d("main", timeLoc.id + ": (" + timeLoc.latitude + ", " + timeLoc.longitude + ") at " + timeLoc.time);
            }
            Log.d("main", "===================================================");
        });
    }

}