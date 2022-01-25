package de.mytrack.mytrackapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.location.Location;
import android.location.LocationManager;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.LocationNetworks;
import de.mytrack.mytrackapp.data.TimeLocation;

public class SmartLocationService extends JobService {

    // TODO: update channel id
    private static final String NOTIFICATION_CHANNEL_ID = "de.mytrack.mytrackapp.channel";
    private static final int SERVICE_ID = 1;

    private static final int JOB_ID = 123;


    private static final long PARAM_RUN_INTERVAL_MS = 10 * 60 * 1000; // 10min
    private static final long PARAM_MAX_LOCATION_AGE_MS = 5 * 60 * 1000; // 5min


    /**
     * Starts the service after 10 seconds.
     * The service will run periodically to fetch the current location.
     */
    public static void startService(@NonNull Context context) {
        startService(context, false, 10 * 1000); // run in the next 10sec
    }

    /**
     * Starts the service after the specified delay.
     * If the service is already scheduled it will be rescheduled with the specified parameters
     * depending on the force parameter.
     * @param force whether the already scheduled job should be overwritten with new params
     * @param delayMs the delay after which the service starts running
     */
    public static void startService(@NonNull Context context, boolean force, long delayMs) {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);

        // fetch all pending jobs and search for our specific
        boolean scheduled = false;
        List<JobInfo> pendingJobs = jobScheduler.getAllPendingJobs();
        for (JobInfo job : pendingJobs) {
            if (JOB_ID == job.getId()) {
                scheduled = true;
                break;
            }
        }

        Log.d("main", "Job scheduled: " + scheduled);

        // do nothing if the job is already scheduled
        if (force || !scheduled) {
            jobScheduler.cancel(JOB_ID);

            Log.d("main", "Scheduling Job: " + JOB_ID);

            // schedule the job
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JOB_ID, new ComponentName(context, SmartLocationService.class));
            JobInfo jobInfo = jobInfoBuilder
                    .setMinimumLatency(delayMs)
                    .setOverrideDeadline(2 * delayMs)
                    .build();

            jobScheduler.schedule(jobInfo);
        }
    }

    private JobParameters mParams = null;

    /**
     * Is called by the system when the job gets scheduled.
     * Returning true means that we have some async work and need more time to finish.
     */
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("main", "onJobStart");

        // start it as a foreground service as a workaround for the location update restrictions
        registerAsForeground();

        mParams = jobParameters;
        doStuff();

        return true;
    }

    /**
     * Fetches the current location and saves it in the database.
     * This method adaptively switches to network location mode when the gps mode is not available.
     * When finished it also stops the service and reschedules the job.
     */
    void doStuff() {
        new FetchGPSLocationTask().getLocation(this).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Location location = task.getResult();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                return new WiFiNetworkFetcher().fetch(latLng, SmartLocationService.this).continueWith(task1 -> location);
            } else {
                return new FetchWiFiLocationTask().getLocation(SmartLocationService.this);
            }
        }).onSuccessTask(this::storeLocation).addOnCompleteListener(task -> finishJob());
    }

    /**
     * A helper class that retrieves the current location based the GPS sensor and the GPS buffer.
     */
    static class FetchGPSLocationTask {

        static class GPSNotAvailableException extends Exception {
        }

        private TaskCompletionSource<Location> mTask = null;
        private Context mContext = null;

        private final OnCompleteListener<Location> onCurrentComplete = task -> {
            if (task.isSuccessful() && task.getResult() != null) {
                Location location = task.getResult();

                long age = getLocationAge(location);
                Log.d("main", "Using new location from " + (age / 1000) + "s");

                // show a message to the user
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                        mContext,
                        "New Location",
                        Toast.LENGTH_LONG).show());


                mTask.setResult(location);
            } else {
                // no location available
                Log.d("main", "No location available...");

                mTask.setException(new GPSNotAvailableException());
            }
        };

        @SuppressLint("MissingPermission")
        private final OnCompleteListener<Location> onLastComplete = task -> {
            Location location = task.getResult();
            long age = getLocationAge(location);

            if (age < PARAM_MAX_LOCATION_AGE_MS) {
                Log.d("main", "Using last known location from " + (age / 1000) + "s");

                // show a message to the user
                new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                        mContext,
                        "Last Location",
                        Toast.LENGTH_LONG).show());

                mTask.setResult(location);
            } else {
                Log.d("main", "Location is too old: " + (age / 1000) + "s");
                Log.d("main", "Fetching new location...");

                FusedLocationProviderClient locationProviderClient = LocationServices.
                        getFusedLocationProviderClient(mContext);
                locationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY,
                        new CancellationTokenSource().getToken()).
                        addOnCompleteListener(onCurrentComplete);
            }
        };

        long getLocationAge(@Nullable Location location) {
            if (location == null)
                return Long.MAX_VALUE;
            else
                return new Date().getTime() - location.getTime();
        }

        @SuppressLint("MissingPermission")
        public Task<Location> getLocation(Context context) {
            mTask = new TaskCompletionSource<>();
            mContext = context;


            FusedLocationProviderClient locationProviderClient = LocationServices.
                    getFusedLocationProviderClient(context);
            locationProviderClient.getLastLocation().addOnCompleteListener(onLastComplete);


            return mTask.getTask();
        }
    }

    /**
     * A helper class that retrieves the current location based on available WiFi network SSIDs.
     */
    static class FetchWiFiLocationTask {

        private TaskCompletionSource<Location> mTask = null;

        private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    context.unregisterReceiver(this);

                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    List<ScanResult> scanResults = wifiManager.getScanResults();

                    List<String> scanSSIDs = new ArrayList<>();
                    for (ScanResult scanResult : scanResults) {
                        scanSSIDs.add(scanResult.SSID);
                    }

                    LiveData<List<LocationNetworks>> locationNetworksData = MyApplication.appContainer.database.locationNetworksDao().getAll();
                    locationNetworksData.observeForever(new Observer<>() {
                        @Override
                        public void onChanged(List<LocationNetworks> locationNetworks) {
                            locationNetworksData.removeObserver(this);

                            float bestMatchValue = 0.0f;
                            LocationNetworks bestMatch = null;

                            // get the best match
                            for (LocationNetworks networks : locationNetworks) {
                                List<String> locationSSIDs = networks.getSSIDs();

                                int maxMatchCount;
                                int matchCount = 0;

                                if (scanSSIDs.size() < locationSSIDs.size()) {
                                    maxMatchCount = scanSSIDs.size();
                                    // loop over scanSSIDs
                                    for (String scanSSID : scanSSIDs) {
                                        if (locationSSIDs.contains(scanSSID)) {
                                            matchCount++;
                                        }
                                    }
                                } else {
                                    maxMatchCount = locationSSIDs.size();
                                    // loop over locationSSIDs
                                    for (String locationSSID : locationSSIDs) {
                                        if (scanSSIDs.contains(locationSSID)) {
                                            matchCount++;
                                        }
                                    }
                                }

                                float matchValue = (float) matchCount / maxMatchCount;

                                if (bestMatch == null || matchValue > bestMatchValue) {
                                    bestMatchValue = matchValue;
                                    bestMatch = networks;
                                }
                            }

                            if (bestMatch != null) {
                                Log.d("main", "Best Match: " + bestMatch.latitude + " " + bestMatch.longitude + " " + bestMatch.ssids);

                                Location location = new Location(LocationManager.NETWORK_PROVIDER);
                                location.setLatitude(bestMatch.latitude);
                                location.setLongitude(bestMatch.longitude);
                                location.setAccuracy(bestMatchValue); // TODO: use the correct measure

                                mTask.setResult(location);
                            } else {
                                Log.d("main", "No Match");
                                mTask.setException(new Exception());
                            }
                        }
                    });
                }
            }
        };

        public Task<Location> getLocation(@NonNull Context context) {
            mTask = new TaskCompletionSource<>();

            // get the location via WiFi history
            context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));

            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (!wifiManager.startScan()) {
                context.unregisterReceiver(wifiScanReceiver);
                // no location updates
                mTask.setException(new Exception());
            }

            return mTask.getTask();
        }

    }

    /**
     * A helper class that stores the available WiFi network SSIDs with the current location.
     * The mapping from SSIDs to locations will be used in the network location mode.
     */
    static class WiFiNetworkFetcher {

        private Area mCurrentArea = null;
        private TaskCompletionSource<Boolean> mTask = null;

        public Task<Boolean> fetch(LatLng location, Context context) {
            mTask = new TaskCompletionSource<>();

            // get area for location
            Utils.getAreaFromLocation(location).addOnCompleteListener(task -> {
                Area result = task.getResult();

                if (result != null) {
                    mCurrentArea = result;

                    context.registerReceiver(wifiScanReceiver, new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    if (!wifiManager.startScan()) {
                        context.unregisterReceiver(wifiScanReceiver);

                        // mark the task as failed
                        mTask.setException(new Exception());
                    }
                } else {
                    // we are currently in no area
                    // therefore we don't need to store the location and ssids
                    mTask.setResult(false);
                }
            });

            return mTask.getTask();
        }

        private final BroadcastReceiver wifiScanReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, @NonNull Intent intent) {
                if (intent.getAction().equals(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION)) {
                    context.unregisterReceiver(this);

                    WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
                    List<ScanResult> scanResults = wifiManager.getScanResults();

                    List<String> ssids = new ArrayList<>();
                    for (ScanResult scanResult : scanResults) {
                        ssids.add(scanResult.SSID);
                    }

                    store(mCurrentArea, ssids);
                }
            }
        };

        private void store(Area area, List<String> ssids) {
            AsyncTask.execute(() -> {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                for (AreaPoint point : area.points) {
                    LatLng latLng = new LatLng(point.latitude, point.longitude);
                    builder.include(latLng);
                }

                LatLng center = builder.build().getCenter();

                LocationNetworks locationNetworks = new LocationNetworks(center, ssids);
                MyApplication.appContainer.database.locationNetworksDao().insertAll(locationNetworks);

                mTask.setResult(true);
            });
        }

    }


    /**
     * A helper method that stores the location in the database.
     * @param location the location to be stored
     * @return a Task Object that can be observed
     */
    @NonNull
    Task<Boolean> storeLocation(Location location) {
        TaskCompletionSource<Boolean> task = new TaskCompletionSource<>();

        TimeLocation timeLocation = new TimeLocation(location);
        AsyncTask.execute(() -> { // use AsyncTask to avoid main thread
            MyApplication.appContainer.database.locationDao().insertAll(timeLocation);
            Log.d("main", "Saved location");
            task.setResult(true);
        });

        return task.getTask();
    }


    /**
     * A helper method that stops the service and reschedules the job.
     */
    void finishJob() {
        // TODO: use adaptive scheduling delay
        startService(this, true, PARAM_RUN_INTERVAL_MS);

        stopForeground(true);
        jobFinished(mParams, false);
    }

    /**
     * This method will be called by the system when the job stops.
     * @return false to indicate that the job should not be automatically rescheduled.
     */
    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }

    /**
     * A helper method that registers this service as a foreground service.
     * This is needed because only foreground services can fetch the location
     * in frequent intervals.
     */
    private void registerAsForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // build the notification
            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("MyTrack")
                    .setContentText("LocationService")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOngoing(true)
                    .build();

            // register foreground service
            createNotificationChannel();
            startForeground(SERVICE_ID, notification);
            Log.d("main", "Foreground service started");
        }
    }

    /**
     * A helper method that creates the notification channel for the
     * foreground service notification. We don't really care about it because the notification
     * will only be visible for a few seconds.
     */
    private void createNotificationChannel() {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // TODO: change strings
            CharSequence name = "Channel Name";
            String description = "Channel Description";
            int importance = NotificationManager.IMPORTANCE_DEFAULT;
            NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }

}
