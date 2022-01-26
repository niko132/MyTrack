package de.mytrack.mytrackapp.export;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.ParcelFileDescriptor;
import android.util.Log;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.SmartLocationService;
import de.mytrack.mytrackapp.data.AppDatabase;
import de.mytrack.mytrackapp.data.LocationDao;
import de.mytrack.mytrackapp.data.TimeLocation;

import androidx.lifecycle.LiveData;

import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.ListIterator;

public class Exporter {
    public static void saveDbToFile(Uri uri, Context context) throws IOException{
        AppDatabase appDatabase = MyApplication.appContainer.database;
        LocationDao locationDao = appDatabase.locationDao();

        AsyncTask.execute(() -> {
            List<TimeLocation> data = locationDao.getAll_2();
            ListIterator<TimeLocation> iterator = data.listIterator();
            try {
                ParcelFileDescriptor pfd = context.getContentResolver().
                        openFileDescriptor(uri, "w");
                FileOutputStream fileOutputStream =
                        new FileOutputStream(pfd.getFileDescriptor());
                fileOutputStream.write(("time;latitude;longitude").getBytes());
                TimeLocation TempTimeLocation = null;
                while (iterator.hasNext()){
                    TempTimeLocation = iterator.next();
                    fileOutputStream.write((
                            TempTimeLocation.time + ";" + TempTimeLocation.latitude + ";" + TempTimeLocation.longitude + "\n"
                    ).getBytes());
                }
                fileOutputStream.close();
                pfd.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        });


    }
}