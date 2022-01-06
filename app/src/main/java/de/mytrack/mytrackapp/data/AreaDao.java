package de.mytrack.mytrackapp.data;

import android.os.AsyncTask;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Transformations;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public abstract class AreaDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long _insertArea(Area area);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract long[] _insertAreaPointList(List<AreaPoint> points);

    @Delete
    abstract void _deleteArea(Area area);

    @Query("DELETE FROM AreaPoint WHERE area_id = :areaId")
    abstract void _deleteAreaPoints(long areaId);

    @Query("DELETE FROM AreaPoint")
    abstract void _deleteAllPoints();

    @Query("DELETE FROM Area")
    abstract void _deleteAllAreas();

    @Query("SELECT * FROM Area WHERE id = :id")
    abstract Area _getArea(int id);

    @Query("SELECT * FROM Area")
    abstract LiveData<List<Area>> _getAllAreas();

    @Query("SELECT * FROM AreaPoint WHERE area_id = :areaId ORDER BY idx ASC")
    abstract List<AreaPoint> _getAreaPointList(long areaId);

    public void insertAreaWithPoints(@NonNull Area area) {
        area.id = _insertArea(area);

        for (int i = 0; i < area.points.size(); i++) {
            area.points.get(i).areaId = area.id;
            area.points.get(i).index = i;
        }

        long[] pointIds = _insertAreaPointList(area.points);
        for (int i = 0; i < area.points.size(); i++) {
            area.points.get(i).id = pointIds[i];
        }
    }

    public void deleteAreaWithPoints(@NonNull Area area) {
        _deleteAreaPoints(area.id);
        _deleteArea(area);
    }

    public void deleteAll() {
        _deleteAllAreas();
        _deleteAllPoints();
    }

    public LiveData<List<Area>> getAllAreasWithPoints() {
        return Transformations.switchMap(_getAllAreas(), input -> {
            MutableLiveData<List<Area>> areaLiveData = new MutableLiveData<>();

            AsyncTask.execute(() -> {
                for (Area area : input) {
                    area.points = _getAreaPointList(area.id);
                }

                areaLiveData.postValue(input);
            });

            return areaLiveData;
        });
    }

}
