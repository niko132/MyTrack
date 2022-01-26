package de.mytrack.mytrackapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationDao {

    @Query("SELECT * FROM TimeLocation ORDER BY time DESC")
    LiveData<List<TimeLocation>> getAll();

    @Query("SELECT * FROM TimeLocation ORDER BY time DESC")
    List<TimeLocation> getAll_2();

    @Query("SELECT * FROM TimeLocation WHERE time BETWEEN :begin AND :end ORDER BY time DESC")
    LiveData<List<TimeLocation>> getAllBetween(long begin, long end);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(TimeLocation... locations);

    @Delete
    void delete(TimeLocation location);

}
