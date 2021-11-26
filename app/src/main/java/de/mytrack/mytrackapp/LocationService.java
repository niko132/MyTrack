package de.mytrack.mytrackapp;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.LifecycleService;

import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.TimeLocation;

public class LocationService extends LifecycleService implements LocationListener {

    private static final String ACTION_STOP_SELF = "de.mytrack.mytrackapp.action.stop_self";

    // TODO: update channel id
    private static final String NOTIFICATION_CHANNEL_ID = "de.mytrack.mytrackapp.channel";
    private static final int SERVICE_ID = 1;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Log.d("main", "onStartCommand");

        if (ACTION_STOP_SELF.equals(intent.getAction())) {
            stopForeground(true);
            stopSelf();
        } else {
            createNotification();
        }

        return START_STICKY;
    }

    @Override
    public void onStart(@Nullable Intent intent, int startId) {
        super.onStart(intent, startId);

        // Do work
        startLocationUpdates();
    }

    @SuppressLint("MissingPermission")
    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d("main", "onDestroy");

        // remove the location listener as a cleanup
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationManager.removeUpdates(this);
    }

    @Nullable
    @Override
    public IBinder onBind(@NonNull Intent intent) {
        super.onBind(intent);
        return null;
    }
    
    @SuppressLint("MissingPermission")
    private void startLocationUpdates() {
        // check for location permissions
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }

        // print database contents when it changes
        AppDatabase.getInstance(this).locationDao().getAll().observe(this, timeLocations -> {
            Log.d("main", "===================================================");
            for (TimeLocation timeLoc : timeLocations) {
                Log.d("main", timeLoc.id + ": (" + timeLoc.latitude + ", " + timeLoc.longitude + ") at " + timeLoc.time);
            }
            Log.d("main", "===================================================");
        });

        // get the LocationManager and register for updates
        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // TODO: maybe use other providers too
        // TODO: update params
        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10 * 60 * 1000, 0, this);
    }

    @Override
    public void onLocationChanged(@NonNull Location location) {
        Log.d("main", "Location changed: " + location.getLatitude() + " " + location.getLongitude());

        // create a new TimeLocation object and insert it into the database
        TimeLocation timeLocation = new TimeLocation(location);
        AsyncTask.execute(() -> { // use AsyncTask to avoid main thread
            AppDatabase.getInstance(LocationService.this).locationDao().insertAll(timeLocation);
        });
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
        // crashes when not implemented
    }

    private void createNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // build intent for stop action
            Intent stopSelfIntent = new Intent(this, LocationService.class);
            stopSelfIntent.setAction(ACTION_STOP_SELF);
            PendingIntent stopIntent = PendingIntent.getService(this, 0, stopSelfIntent, PendingIntent.FLAG_CANCEL_CURRENT);

            // build the notification
            Notification notification = new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
                    .setContentTitle("MyTrack")
                    .setContentText("LocationService")
                    .setSmallIcon(R.drawable.ic_launcher_foreground)
                    .setOngoing(true)
                    .addAction(0, "Stop", stopIntent)
                    .build();

            // register foreground service
            createNotificationChannel();
            startForeground(SERVICE_ID, notification);
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
