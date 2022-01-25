package de.mytrack.mytrackapp.data;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LocationNetworksDao {

    @Query("SELECT * FROM LocationNetworks")
    LiveData<List<LocationNetworks>> getAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(LocationNetworks... locationNetworks);

    @Delete
    void delete(LocationNetworks locationNetworks);

}
