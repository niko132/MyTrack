package de.mytrack.mytrackapp;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import android.content.ComponentName;
import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.common.api.internal.TaskUtil;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.TaskCompletionSource;
import com.google.android.gms.tasks.Tasks;

import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutionException;

import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.TimeLocation;

public class SmartLocationService extends JobService {

    // TODO: update channel id
    private static final String NOTIFICATION_CHANNEL_ID = "de.mytrack.mytrackapp.channel";
    private static final int SERVICE_ID = 1;

    private static final int JOB_ID = 123;


    private static final long PARAM_RUN_INTERVAL_MS = 10 * 60 * 1000; // 10min
    private static final long PARAM_MAX_LOCATION_AGE_MS = 5 * 60 * 1000; // 5min


    public static void startService(@NonNull Context context) {
        startService(context, false, 10 * 1000); // run in the next 10sec
    }

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

    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Log.d("main", "onJobStart");

        // start it as a foreground service as a workaround for the location update restrictions
        registerAsForeground();

        mParams = jobParameters;
        doStuff();

        return true;
    }


    private final OnCompleteListener<Location> onCurrentComplete = task -> {
        if (task.isSuccessful() && task.getResult() != null) {
            Location location = task.getResult();

            long age = getAge(location);
            Log.d("main", "Using new location from " + (age / 1000) + "s");

            // show a message to the user
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                    SmartLocationService.this,
                    "New Location",
                    Toast.LENGTH_LONG).show());

            // save the location to the database
            storeLocation(location).addOnCompleteListener(task1 -> finishJob());
        } else {
            // no location available
            Log.d("main", "No location available...");
        }

        finishJob();
    };

    @SuppressLint("MissingPermission")
    private final OnCompleteListener<Location> onLastComplete = task -> {
        Location location = task.getResult();
        long age = getAge(location);

        if (age < PARAM_MAX_LOCATION_AGE_MS) {
            Log.d("main", "Using last known location from " + (age / 1000) + "s");

            // show a message to the user
            new Handler(Looper.getMainLooper()).post(() -> Toast.makeText(
                    SmartLocationService.this,
                    "Last Location",
                    Toast.LENGTH_LONG).show());

            // save the location to the database
            storeLocation(location).addOnCompleteListener(task1 -> finishJob());
        } else {
            Log.d("main", "Location is too old: " + (age / 1000) + "s");
            Log.d("main", "Fetching new location...");

            FusedLocationProviderClient locationProviderClient = LocationServices.
                    getFusedLocationProviderClient(SmartLocationService.this);
            locationProviderClient.getCurrentLocation(LocationRequest.PRIORITY_HIGH_ACCURACY,
                    new CancellationTokenSource().getToken()).
                    addOnCompleteListener(onCurrentComplete);
        }
    };


    @SuppressLint("MissingPermission")
    void doStuff() {
        FusedLocationProviderClient locationProviderClient = LocationServices.
                getFusedLocationProviderClient(this);
        locationProviderClient.getLastLocation().addOnCompleteListener(onLastComplete);
    }

    Task<Boolean> storeLocation(Location location) {
        TaskCompletionSource<Boolean> task = new TaskCompletionSource<>();

        TimeLocation timeLocation = new TimeLocation(location);
        AsyncTask.execute(() -> { // use AsyncTask to avoid main thread
            AppDatabase.getInstance(SmartLocationService.this).locationDao().insertAll(timeLocation);
            Log.d("main", "Saved location");
            task.setResult(true);
        });

        return task.getTask();
    }

    long getAge(@Nullable Location location) {
        if (location == null)
            return Long.MAX_VALUE;
        else
            return new Date().getTime() - location.getTime();
    }

    void finishJob() {
        // TODO: use adaptive scheduling delay
        startService(this, true, PARAM_RUN_INTERVAL_MS);

        stopForeground(true);
        jobFinished(mParams, false);
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return false;
    }


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
