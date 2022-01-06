package de.mytrack.mytrackapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface CustomActivityDao {

    @Query("SELECT * FROM CustomActivity")
    LiveData<List<CustomActivity>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(CustomActivity... activities);

    @Delete
    void delete(CustomActivity activity);

}
