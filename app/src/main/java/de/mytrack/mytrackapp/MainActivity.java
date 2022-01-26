package de.mytrack.mytrackapp;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;
import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCaller;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContract;
import androidx.activity.result.contract.ActivityResultContracts;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.databinding.ActivityMainBinding;
import de.mytrack.mytrackapp.export.Exporter;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private int CREATE_FILE_REQUEST_CODE = 1234;

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

        // TODO - remove later
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/txt");
        intent.putExtra(Intent.EXTRA_TITLE, "mytrack.txt");
        startActivityForResult(intent, CREATE_FILE_REQUEST_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        super.onActivityResult(requestCode, resultCode, resultData);
        if (requestCode == CREATE_FILE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            Uri uri = null;
            if (resultData != null) {
                uri = resultData.getData();
                try {
                    Exporter.saveDbToFile(uri, getApplicationContext());
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
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
        AppDatabase.getInstance(this).locationDao().getAll().observe(this, timeLocations -> {
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