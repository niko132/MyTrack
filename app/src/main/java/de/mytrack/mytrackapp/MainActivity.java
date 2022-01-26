package de.mytrack.mytrackapp;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import java.util.Map;

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

        requestLocationPermission();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    private void requestLocationPermission() {
        new LocationPermissionRequest().requestPermissions(this, (coarse, fine, background) -> {
            Log.d("main", "onPermissionResult");
            Log.d("main", coarse + " " + fine + " " + background);

            if (coarse || fine || background) {
                startLocationService();
            }
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
        MyApplication.appContainer.database.locationDao().getAll().observe(this, timeLocations -> {
            Log.d("main", "===================================================");
            for (TimeLocation timeLoc : timeLocations) {
                Log.d("main", timeLoc.id + ": (" + timeLoc.latitude + ", " + timeLoc.longitude + ") at " + timeLoc.time);
            }
            Log.d("main", "===================================================");
        });
    }


    private static class LocationPermissionRequest {
        private interface PermissionCallback {
            void onPermissionResult(boolean coarse, boolean fine, boolean background);
        }

        private PermissionCallback mCallback = null;

        private ActivityResultLauncher<String> mBackgroundLauncher = null;
        private ActivityResultLauncher<String[]> mForegroundLauncher = null;

        private boolean mCoarseGranted = false;
        private boolean mFineGranted = false;
        private boolean mBackgroundGranted = false;

        public void requestPermissions(@NonNull AppCompatActivity activity, @NonNull PermissionCallback callback) {
            mCallback = callback;

            mBackgroundLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestPermission(), result -> {
                mBackgroundLauncher.unregister();

                mBackgroundGranted = getGranted(result);

                if (!mBackgroundGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                            // TODO: show explanation
                            mCallback.onPermissionResult(mCoarseGranted, mFineGranted, mBackgroundGranted);
                        } else {
                            // TODO: never ask again
                            mCallback.onPermissionResult(mCoarseGranted, mFineGranted, mBackgroundGranted);
                        }
                    } else {
                        mCallback.onPermissionResult(mCoarseGranted, mFineGranted, mBackgroundGranted);
                    }
                } else {
                    // continue
                    mCallback.onPermissionResult(mCoarseGranted, mFineGranted, mBackgroundGranted);
                }
            });

            mForegroundLauncher = activity.registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                mForegroundLauncher.unregister();

                mCoarseGranted = getGranted(result, Manifest.permission.ACCESS_COARSE_LOCATION);
                mFineGranted = getGranted(result, Manifest.permission.ACCESS_FINE_LOCATION);

                if (!mCoarseGranted && !mFineGranted) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                        if (activity.shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION)) {
                            // TODO: show explanation to the user
                            mCallback.onPermissionResult(mCoarseGranted, mFineGranted, false);
                        } else {
                            // TODO: never ask again
                            mCallback.onPermissionResult(mCoarseGranted, mFineGranted, false);
                        }
                    } else {
                        requestBackgroundPermission(activity);
                    }
                } else {
                    requestBackgroundPermission(activity);
                }
            });


            requestForegroundPermissions(activity);
        }


        private void requestBackgroundPermission(AppCompatActivity activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                mBackgroundGranted = getGranted(activity, Manifest.permission.ACCESS_BACKGROUND_LOCATION);

                if (!mBackgroundGranted) {
                    // request permission
                    // TODO: change strings
                    new AlertDialog.Builder(activity)
                            .setTitle("Allow Background Location")
                            .setMessage("To gather and process your location data, we need to access your location even while the app is in the background. Please grant access to achieve full functionality.")
                            .setPositiveButton("Ok", (dialogInterface, i) -> {
                                mBackgroundLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION);
                                dialogInterface.dismiss();
                            })
                            .setNegativeButton("Hell no", (dialogInterface, i) -> {
                                // TODO: count dismisses and don't show
                                dialogInterface.dismiss();
                                mCallback.onPermissionResult(mCoarseGranted, mFineGranted, mBackgroundGranted);
                            })
                            .create()
                            .show();
                } else {
                    // continue
                    mCallback.onPermissionResult(mCoarseGranted, mFineGranted, mBackgroundGranted);
                }
            } else {
                mCallback.onPermissionResult(true, true, true);
            }
        }

        private void requestForegroundPermissions(AppCompatActivity activity) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                mCoarseGranted = getGranted(activity, Manifest.permission.ACCESS_COARSE_LOCATION);
                mFineGranted = getGranted(activity, Manifest.permission.ACCESS_FINE_LOCATION);

                if (!mCoarseGranted && !mFineGranted) {
                    // request permission
                    mForegroundLauncher.launch(new String[]{
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION
                    });
                } else {
                    // continue
                    requestBackgroundPermission(activity);
                }
            } else {
                mCallback.onPermissionResult(true, true, true);
            }
        }


        private boolean getGranted(@NonNull Map<String, Boolean> result, String permission) {
            if (!result.containsKey(permission))
                return false;

            return getGranted(result.get(permission));
        }

        private boolean getGranted(Boolean value) {
            return value != null && value;
        }

        private boolean getGranted(Context context, String permission) {
            return ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED;
        }

    }

}