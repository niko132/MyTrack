package de.mytrack.mytrackapp.ui.areas;

import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

import com.github.dhaval2404.colorpicker.MaterialColorPickerDialog;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;

import de.mytrack.mytrackapp.MyApplication;
import de.mytrack.mytrackapp.R;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.data.TimeLocation;
import de.mytrack.mytrackapp.databinding.FragmentAreasBinding;

public class AreasFragment extends Fragment implements OnMapReadyCallback {

    // create a helper class for the polygons to be drawn on the map
    private static class DraggablePolygon {
        public List<Marker> mMarkers;
        public Polygon mPolygon;
        private Area mArea;

        public DraggablePolygon(@NonNull GoogleMap map, @NonNull Area area) {
            mMarkers = new ArrayList<>();
            mArea = area;

            int color = mArea.color;
            int fillColor = Color.argb(128, Color.red(color), Color.green(color), Color.blue(color));

            // create polygon and markers for the map
            PolygonOptions polygonOptions = new PolygonOptions()
                    .geodesic(true)
                    .clickable(true)
                    .fillColor(fillColor)
                    .strokeColor(color);

            for (AreaPoint point : area.points) {
                LatLng corner = new LatLng(point.latitude, point.longitude);
                polygonOptions.add(corner);

                Marker marker = map.addMarker(new MarkerOptions()
                        .position(corner)
                        .anchor(0.5f, 0.5f)
                        .draggable(true)
                        .visible(false)
                        .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_area_marker_24dp)));
                marker.setTag(this);
                mMarkers.add(marker);
            }

            mPolygon = map.addPolygon(polygonOptions);
            mPolygon.setTag(this);
        }

        public void setMarkerVisible(boolean visible) {
            if (!isValid())
                return;

            for (Marker marker : mMarkers) {
                marker.setVisible(visible);
            }
        }

        public void deleteMarker(Marker marker) {
            mMarkers.remove(marker);
            marker.remove();
            updatePolygon();
        }

        // keep markers and polygon in sync
        public void updatePolygon() {
            if (!isValid())
                return;

            // TODO: implement safety (list sizes, ...)
            List<LatLng> polygonPoints = mPolygon.getPoints();
            for (int i = 0; i < mMarkers.size(); i++) {
                polygonPoints.set(i, mMarkers.get(i).getPosition());
                mArea.points.get(i).latitude = mMarkers.get(i).getPosition().latitude;
                mArea.points.get(i).longitude = mMarkers.get(i).getPosition().longitude;
            }
            mPolygon.setPoints(polygonPoints);
        }

        // called when a polygon was clicked
        // zooms onto the polygon and enables 'edit mode'
        public void focus(GoogleMap map) {
            if (!isValid())
                return;

            LatLngBounds.Builder tmp = new LatLngBounds.Builder();
            for (Marker marker : mMarkers) {
                tmp.include(marker.getPosition());
            }

            LatLngBounds tmpBounds = tmp.build();
            LatLng center = tmpBounds.getCenter();
            double centLat = center.latitude;
            double centLong = center.longitude;

            // current version just scales the bounds to center the area in the top section
            // TODO: improve calculation (big areas get cut off at the bottom)
            double newSouthWestLat = centLat + (tmpBounds.southwest.latitude - centLat) * 4.0;
            double newSouthWestLong = centLong + (tmpBounds.southwest.longitude - centLong) * 1.5;

            double newNorthEastLat = centLat + (tmpBounds.northeast.latitude - centLat) * 1.5;
            double newNorthEastLong = centLong + (tmpBounds.northeast.longitude - centLong) * 1.5;

            LatLng newSouthWest = new LatLng(newSouthWestLat, newSouthWestLong);
            LatLng newNorthEast = new LatLng(newNorthEastLat, newNorthEastLong);

            LatLngBounds bounds = new LatLngBounds(newSouthWest, newNorthEast);

            map.animateCamera(CameraUpdateFactory.newLatLngBounds(bounds, 0), 300, null);
        }

        // called when an area is removed
        // removes all the markers and the polygon from the map
        public void delete() {
            if (!isValid())
                return;

            mPolygon.remove();
            for (Marker marker : mMarkers) {
                marker.remove();
            }

            mMarkers = null;
            mPolygon = null;
            mArea = null;
        }

        public Area getArea() {
            return mArea;
        }

        private boolean isValid() {
            return mPolygon != null && mMarkers != null && mArea != null;
        }
    }

    private AreasViewModel areasViewModel;
    private FragmentAreasBinding binding;

    private GoogleMap mMap = null;

    // hold a list of all shown polygons
    private final List<DraggablePolygon> mPolygons = new ArrayList<>();
    private DraggablePolygon mCurrentFocused = null;

    // only used for debugging purposes
    // TODO: remove
    private final List<Marker> mHistoryMarkers = new ArrayList<>();


    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        areasViewModel =
                new ViewModelProvider(this).get(AreasViewModel.class);

        binding = FragmentAreasBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(areasViewModel);

        binding.fab.setOnClickListener(view -> {
            if (mMap != null) {
                // add a new area in the center of the screen
                LatLngBounds visibleBounds = mMap.getProjection().getVisibleRegion().latLngBounds;
                LatLng center = visibleBounds.getCenter();

                double latDist = Math.abs(visibleBounds.northeast.latitude - visibleBounds.southwest.latitude);
                double longDist = Math.abs(visibleBounds.northeast.longitude - visibleBounds.southwest.longitude);

                double quarterLat = latDist / 4.0;
                double quarterLong = longDist / 4.0;

                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                builder.include(new LatLng(center.latitude - quarterLat, center.longitude - quarterLong));
                builder.include(new LatLng(center.latitude - quarterLat, center.longitude + quarterLong));
                builder.include(new LatLng(center.latitude + quarterLat, center.longitude + quarterLong));
                builder.include(new LatLng(center.latitude + quarterLat, center.longitude - quarterLong));

                int[] colors = getResources().getIntArray(R.array.background_colors);
                int color = colors[(int) (System.currentTimeMillis() % colors.length)];

                areasViewModel.onAddArea(getResources().getString(R.string.areas_new_area_name),
                        color, builder.build());
            }
        });

        binding.areaTitleText.setOnEditorActionListener((textView, actionId, keyEvent) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (mCurrentFocused != null && mCurrentFocused.isValid()) {
                    mCurrentFocused.getArea().name = textView.getText().toString();
                    areasViewModel.onAreaChanged(mCurrentFocused.getArea());

                    textView.clearFocus();
                }
            }
            return false;
        });

        binding.areaColorFrame.setOnClickListener(view -> new MaterialColorPickerDialog.Builder(view.getContext())
                .setColorRes(getResources().getIntArray(R.array.background_colors))
                .setColorListener((color, colorHex) -> {
                    if (mCurrentFocused != null && mCurrentFocused.isValid()) {
                        mCurrentFocused.getArea().color = color;
                        areasViewModel.onAreaChanged(mCurrentFocused.getArea());
                    }
                })
                .show());

        binding.areaAddReminderBtn.setOnClickListener(view -> {
            // TODO: implement reminder
            Log.d("main", "Reminder not yet implemented");
            Toast.makeText(AreasFragment.this.getActivity(), "Reminder not yet implemented", Toast.LENGTH_SHORT).show();
        });

        binding.areaDeleteBtn.setOnClickListener(view -> {
            if (mCurrentFocused == null || !mCurrentFocused.isValid())
                return;

            Area area = mCurrentFocused.getArea();
            areasViewModel.onDeleteArea(area);
        });



        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return binding.getRoot();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mMap = googleMap;

        mMap.setOnPolygonClickListener(polygon -> {
            // search for clicked polygon object and focus on it
            for (DraggablePolygon dragPoly : mPolygons) {
                if (polygon.equals(dragPoly.mPolygon)) {
                    focusOn(dragPoly, true);
                    break;
                }
            }
        });

        // unfocus the current polygon
        mMap.setOnMapClickListener(latLng -> focusOn(null, true));

        mMap.setOnMapLongClickListener(latLng -> {
            // insert a new point into the polygon on long click
            if (mCurrentFocused != null && mCurrentFocused.isValid()) {
                // TODO: improve algorithm
                // at the moment it places a new point between the 2 closest points
                // this sometimes leads to unexpected connections of points
                // better calculate the distance to the edges and do some bounding border

                int lowestIndex = -1;
                int secondLowestIndex = -1;

                float lowestDistance = Float.MAX_VALUE;
                float secondLowestDistance = Float.MAX_VALUE;

                for (int i = 0; i < mCurrentFocused.getArea().points.size(); i++) {
                    AreaPoint current = mCurrentFocused.getArea().points.get(i);
                    float[] tmp = new float[3];
                    Location.distanceBetween(current.latitude, current.longitude, latLng.latitude, latLng.longitude, tmp);
                    float dist = tmp[0];

                    if (dist < lowestDistance) {
                        secondLowestIndex = lowestIndex;
                        lowestIndex = i;
                        secondLowestDistance = lowestDistance;
                        lowestDistance = dist;
                    } else if (dist < secondLowestDistance) {
                        secondLowestIndex = i;
                        secondLowestDistance = dist;
                    }
                }

                int idx = Math.max(lowestIndex, secondLowestIndex);
                // handle the case when you insert a point between the first and the last point
                if (Math.min(lowestIndex, secondLowestIndex) == 0)
                    idx += 1;
                mCurrentFocused.getArea().points.add(idx, new AreaPoint(latLng.latitude, latLng.longitude));
                areasViewModel.onAreaChanged(mCurrentFocused.getArea());
            }
        });

        // delete point on marker single click
        mMap.setOnMarkerClickListener(marker -> {
            DraggablePolygon dragPoly = findDragPoly(marker);
            if (dragPoly != null) {
                dragPoly.deleteMarker(marker);
                areasViewModel.onAreaChanged(dragPoly.getArea());
            }

            return true;
        });

        // update the polygon when a marker is dragged
        mMap.setOnMarkerDragListener(new GoogleMap.OnMarkerDragListener() {
            @Override
            public void onMarkerDrag(@NonNull Marker marker) {
                updatePolygon(marker);
            }

            @Override
            public void onMarkerDragEnd(@NonNull Marker marker) {
                updatePolygon(marker);

                DraggablePolygon dragPoly = findDragPoly(marker);
                if (dragPoly != null) {
                    // save new positions at the end of the drag
                    areasViewModel.onAreaChanged(dragPoly.getArea());
                }
            }

            @Override
            public void onMarkerDragStart(@NonNull Marker marker) {
                updatePolygon(marker);
            }
        });

        // receive updates when the area data changes
        // this is called whenever an area gets inserted, changed or deleted in the database
        // Areas may get new id's even if they weren't changed
        areasViewModel.getAreas().observe(getViewLifecycleOwner(), areas -> {
            // find the new Area object which represents the currently focused area
            Area newFocused = null;
            if (mCurrentFocused != null) {
                for (Area area : areas) {
                    if (area.pointsEqual(mCurrentFocused.getArea())) {
                        newFocused = area;
                        break;
                    }
                }
            }

            // handle removed areas
            List<DraggablePolygon> deletes = new ArrayList<>();
            for (DraggablePolygon dragPoly : mPolygons) {
                boolean found = false;

                for (Area area : areas) {
                    if (area.equals(dragPoly.getArea())) {
                        found = true;
                        break;
                    }
                }

                if (!found) {
                    deletes.add(dragPoly);
                    dragPoly.delete();
                }
            }
            mPolygons.removeAll(deletes);

            // and handle new areas
            for (Area area : areas) {
                if (findDragPoly(area) == null) {
                    DraggablePolygon dragPoly = new DraggablePolygon(mMap, area);
                    mPolygons.add(dragPoly);

                    if (area == newFocused) {
                        focusOn(dragPoly, false);
                    }
                }
            }

            // no focused area anymore - unfocus
            if (newFocused == null)
                focusOn(null, true);
        });



        /*
        The following code is used to visualize the tracked location data of the current day.
        It adds a Marker for every position and even listens for new updates.
        This code will probably be deleted in the following versions
         */

        // today
        Calendar begin = new GregorianCalendar();

        // show the day before n days
        begin.add(Calendar.DAY_OF_MONTH, 0);

        // reset hour, minutes, seconds and millis
        begin.set(Calendar.HOUR_OF_DAY, 0);
        begin.set(Calendar.MINUTE, 0);
        begin.set(Calendar.SECOND, 0);
        begin.set(Calendar.MILLISECOND, 0);

        Calendar end = (Calendar) begin.clone();

        // next day
        end.add(Calendar.DAY_OF_MONTH, 1);

        MyApplication.appContainer.database.locationDao().getAllBetween(begin.getTime().getTime(), end.getTime().getTime()).observe(getViewLifecycleOwner(), timeLocations -> {
            Log.d("main", "Fetched " + timeLocations.size() + " positions");

            for (Marker marker : mHistoryMarkers) {
                marker.remove();
            }
            mHistoryMarkers.clear();

            for (TimeLocation location : timeLocations) {
                LatLng latLng = new LatLng(location.latitude, location.longitude);

                Marker marker = mMap.addMarker(new MarkerOptions()
                        .position(latLng));
                mHistoryMarkers.add(marker);
            }
        });
    }

    // helper function to find a DraggablePolygon with an Area
    private @Nullable DraggablePolygon findDragPoly(Area area) {
        for (DraggablePolygon dragPoly : mPolygons) {
            if (area.equals(dragPoly.getArea()))
                return dragPoly;
        }

        return null;
    }

    // helper function to find a DraggablePolygon with a Marker
    private @Nullable DraggablePolygon findDragPoly(@NonNull Marker marker) {
        return (DraggablePolygon) marker.getTag();
    }

    // handles focus change
    // unfocuses old polygon and focuses the new
    // also shows/hides the bottom sheet and the fab
    private void focusOn(@Nullable DraggablePolygon dragPoly, boolean move) {
        if (mCurrentFocused != null) {
            mCurrentFocused.setMarkerVisible(false);
        }
        mCurrentFocused = dragPoly;

        if (dragPoly != null) {
            dragPoly.setMarkerVisible(true);

            if (move)
                dragPoly.focus(mMap);
        } else if (move) {
            mMap.animateCamera(CameraUpdateFactory.zoomOut(), 300, null);
        }

        areasViewModel.onFocus(dragPoly != null ? dragPoly.getArea() : null);
        TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());
    }

    // helper function to find and update a DraggablePolygon with a Marker
    private void updatePolygon(@NonNull Marker marker) {
        DraggablePolygon dragPoly = findDragPoly(marker);
        if (dragPoly != null) {
            dragPoly.updatePolygon();
        }
    }
}