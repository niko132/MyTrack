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
import android.os.BatteryManager;
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
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.SuccessContinuation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaDao;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.LocationNetworks;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.spheric_geometry.PointOnSphere;
import de.mytrack.mytrackapp.spheric_geometry.PolygonOnEarth;

public class SmartLocationService extends JobService {

    // TODO: update channel id
    private static final String NOTIFICATION_CHANNEL_ID = "de.mytrack.mytrackapp.channel";
    private static final int SERVICE_ID = 1;

    private static final int JOB_ID = 123;


    private static final long PARAM_RUN_INTERVAL_MS = 10 * 60 * 1000; // 10min
    private static final long PARAM_MAX_LOCATION_AGE_MS = 3 * 60 * 1000; // 3min


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
     *
     * @param force   whether the already scheduled job should be overwritten with new params
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

            // create a time window around the delay time
            long minLatency = (long) (delayMs * 0.75);
            long overrideDeadline = (long) (delayMs * 1.25);

            // schedule the job
            JobInfo.Builder jobInfoBuilder = new JobInfo.Builder(JOB_ID, new ComponentName(context, SmartLocationService.class));
            JobInfo jobInfo = jobInfoBuilder
                    // .setMinimumLatency(delayMs)
                    // .setOverrideDeadline(2 * delayMs)
                    .setMinimumLatency(minLatency)
                    .setOverrideDeadline(overrideDeadline)
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
        /*
        new FetchGPSLocationTask().getLocation(this, false).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Location location = task.getResult();
                LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());

                return new WiFiNetworkFetcher().fetch(latLng, SmartLocationService.this).continueWith(task1 -> location);
            } else {
                return new FetchWiFiLocationTask().getLocation(SmartLocationService.this);
            }
        }).onSuccessTask(this::storeLocation).addOnCompleteListener(task -> finishJob(task.getResult()));
        */


        // procedure:
        // 1. Fetch GPS location (with buffered)
        //      1.1 onSuccess: Validate location
        //          1.1.1 onSuccess: Store available SSIDs; return location back to 1.
        //          1.1.2 onFailure: return failure to 1.
        //      1.2 onFailure: return failure to 1.
        // 2. if 1. succeeded, Store location
        // 3. Finish the job

        new FetchGPSLocationTask().getLocation(this, false).continueWithTask(task -> {
            if (task.isSuccessful()) {
                Location gpsLocation = task.getResult();

                return validateLocation(gpsLocation, SmartLocationService.this).continueWithTask(task12 -> {
                    if (task12.isSuccessful()) {
                        Toast.makeText(SmartLocationService.this, "Validation succeeded", Toast.LENGTH_SHORT).show();

                        Location validationLocation = task12.getResult();
                        LatLng latLng = new LatLng(validationLocation.getLatitude(), validationLocation.getLongitude());

                        return new WiFiNetworkFetcher().fetch(latLng, SmartLocationService.this).continueWith(task1 -> validationLocation);
                    } else {
                        Toast.makeText(SmartLocationService.this, "Validation failed", Toast.LENGTH_SHORT).show();
                        return Tasks.forException(new Exception());
                    }
                });
            } else {
                return new FetchWiFiLocationTask().getLocation(SmartLocationService.this);
            }
        }).onSuccessTask(this::storeLocation).addOnCompleteListener(task -> {
            Location result = task.isSuccessful() ? task.getResult() : null;
            finishJob(result);
        });
    }

    /**
     * Validates a new location.
     * First it checks whether validation is needed (only if moved to a different area).
     * If yes it fetches another location and validates the area against the other area.
     * If the areas are the same, the location is validated and gets passed back.
     * Otherwise a failure will be passed back.
     * @param location the location to validate
     * @param context
     * @return a task that can be observed
     */
    @NonNull
    private Task<Location> validateLocation(Location location, Context context) {
        TaskCompletionSource<Location> validationTask = new TaskCompletionSource<>();

        Utils.getAreaFromLastLocation().addOnCompleteListener(task12 -> {
            Area lastArea = task12.getResult();

            Utils.getAreaFromLocation(new LatLng(location.getLatitude(), location.getLongitude())).addOnCompleteListener(task1 -> {
                Area newArea = task1.getResult();

                if ((lastArea != null && lastArea.equals(newArea)) || (lastArea == null && newArea == null)) {
                    // areas are the same
                    validationTask.setResult(location);
                } else {
                    // areas are different
                    new Handler().postDelayed(() -> new FetchGPSLocationTask().getLocation(context, true).addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Location validationLocation = task.getResult();
                            Utils.getAreaFromLocation(new LatLng(validationLocation.getLatitude(), validationLocation.getLongitude())).addOnCompleteListener(task2 -> {
                                Area validationArea = task2.getResult();

                                if ((newArea != null && newArea.equals(validationArea)) || (newArea == null && validationArea == null)) {
                                    // areas are the same
                                    validationTask.setResult(validationLocation);
                                } else {
                                    validationTask.setException(new Exception());
                                }
                            });
                        } else {
                            validationTask.setException(new Exception());
                        }
                    }), 30 * 1000); // get a validation point after 30 seconds
                }
            });
        });

        return validationTask.getTask();
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
        public Task<Location> getLocation(Context context, boolean gpsOnly) {
            mTask = new TaskCompletionSource<>();
            mContext = context;

            if (gpsOnly) {
                FusedLocationProviderClient locationProviderClient = LocationServices.
                        getFusedLocationProviderClient(context);
                locationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY,
                        new CancellationTokenSource().getToken()).
                        addOnCompleteListener(onCurrentComplete);
            } else {
                FusedLocationProviderClient locationProviderClient = LocationServices.
                        getFusedLocationProviderClient(context);
                locationProviderClient.getLastLocation().addOnCompleteListener(onLastComplete);
            }

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
                                float accuracy = bestMatchValue; // TODO: use the correct measure

                                LiveData<Area> areaData = MyApplication.appContainer.database.areaDao().getAreaWithPoints(bestMatch.areaId);
                                areaData.observeForever(new Observer<>() {
                                    @Override
                                    public void onChanged(Area area) {
                                        areaData.removeObserver(this);

                                        LatLngBounds.Builder builder = new LatLngBounds.Builder();
                                        for (AreaPoint point : area.points) {
                                            builder.include(new LatLng(point.latitude, point.longitude));
                                        }

                                        LatLng center = builder.build().getCenter();

                                        Log.d("main", "Best Match: " + area.name);

                                        Location location = new Location(LocationManager.NETWORK_PROVIDER);
                                        location.setLatitude(center.latitude);
                                        location.setLongitude(center.longitude);
                                        location.setAccuracy(accuracy);

                                        mTask.setResult(location);
                                    }
                                });
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
                LocationNetworks locationNetworks = new LocationNetworks(area, ssids);
                MyApplication.appContainer.database.locationNetworksDao().insertAll(locationNetworks);

                mTask.setResult(true);
            });
        }

    }


    /**
     * A helper method that stores the location in the database.
     *
     * @param location the location to be stored
     * @return a Task Object that can be observed
     */
    @NonNull
    Task<Location> storeLocation(Location location) {
        TaskCompletionSource<Location> task = new TaskCompletionSource<>();

        TimeLocation timeLocation = new TimeLocation(location);
        AsyncTask.execute(() -> { // use AsyncTask to avoid main thread
            MyApplication.appContainer.database.locationDao().insertAll(timeLocation);
            Log.d("main", "Saved location");
            task.setResult(location);
        });

        return task.getTask();
    }


    /**
     * A helper method that stops the service and reschedules the job.
     */
    private void finishJob(@Nullable Location location) {
        // TODO: use adaptive scheduling delay

        final double defaultSpeed = 50.0; // in km/h

        Task<Long> minDistTask = getAreasSamplingTime(location, defaultSpeed); // assume max speed of 50km/h
        Task<Long> batteryTask = getBatterySamplingTime();

        Tasks.whenAllComplete(
                minDistTask,
                batteryTask
        ).addOnCompleteListener(tasks -> {
            // constrain the distance time to the battery time
            long delay = Math.max(minDistTask.getResult(), batteryTask.getResult());

            startService(SmartLocationService.this, true, delay);

            stopForeground(true);
            jobFinished(mParams, false);
        });
    }

    @NonNull
    private Task<Long> getBatterySamplingTime() {
        IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = registerReceiver(null, intentFilter);

        int status = batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, -1);
        boolean isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                status == BatteryManager.BATTERY_STATUS_FULL;

        int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        float batteryPct = level * 100 / (float) scale;

        Log.d("myTrack", "Battery: " + level + " " + scale + " " + batteryPct);

        TaskCompletionSource<Long> task = new TaskCompletionSource<>();
        AsyncTask.execute(() -> {
            int timeInMin;

            if (isCharging || batteryPct > 50.0f)
                timeInMin = MyApplication.appContainer.database.settingsDao().getNormalDelayTime();
            else if (batteryPct > 30.0f)
                timeInMin = MyApplication.appContainer.database.settingsDao().getLowDelayTime();
            else
                timeInMin = MyApplication.appContainer.database.settingsDao().getCriticalDelayTime();

            task.setResult(timeInMin * 60L * 1000L);
        });

        return task.getTask();
    }

    @NonNull
    private Task<Long> getAreasSamplingTime(@Nullable Location location, double speed) {
        TaskCompletionSource<Long> task = new TaskCompletionSource<>();

        if (location == null) {
            // if we don't know the location assume we are very close to an area
            task.setResult(0L);
        } else {
            LiveData<List<Area>> areasData = MyApplication.appContainer.database.areaDao().getAllAreasWithPoints();
            areasData.observeForever(new Observer<>() {
                @Override
                public void onChanged(List<Area> areas) {
                    areasData.removeObserver(this);

                    double minDistance = Double.MAX_VALUE;

                    PointOnSphere point = new PointOnSphere(location.getLatitude(), location.getLongitude());
                    PolygonOnEarth[] polygons = PolygonOnEarth.from(areas);
                    for (PolygonOnEarth polygon : polygons) {
                        // we don't need negative distances in this case
                        double distance = Math.abs(polygon.distance(point));

                        if (distance < minDistance) {
                            minDistance = distance;
                        }
                    }

                    task.setResult((long) (minDistance * 60 * 60 * 1000 / speed));
                }
            });
        }

        return task.getTask();
    }

    /**
     * This method will be called by the system when the job stops.
     *
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
