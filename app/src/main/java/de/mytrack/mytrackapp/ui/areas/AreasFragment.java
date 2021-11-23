package de.mytrack.mytrackapp.ui.areas;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.transition.TransitionManager;

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
import java.util.List;

import de.mytrack.mytrackapp.R;
import de.mytrack.mytrackapp.databinding.FragmentAreasBinding;

public class AreasFragment extends Fragment implements OnMapReadyCallback {

    // create a helper class for the polygons to be drawn on the map
    private static class DraggablePolygon {
        public List<Marker> mMarkers;
        public Polygon mPolygon;
        private AreasViewModel.Area mArea;

        public DraggablePolygon(@NonNull GoogleMap map, @NonNull AreasViewModel.Area area) {
            mMarkers = new ArrayList<>();
            mArea = area;

            // create polygon and markers for the map
            PolygonOptions polygonOptions = new PolygonOptions()
                    .clickable(true)
                    .fillColor(0x8020b020)
                    .strokeColor(0xff20b020);

            for (LatLng corner : area.points) {
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
            for (Marker marker : mMarkers) {
                marker.setVisible(visible);
            }
        }

        public void updatePolygon() {
            // keep markers and polygon in sync
            List<LatLng> points = new ArrayList<>();
            for (Marker m : mMarkers) {
                points.add(m.getPosition());
            }

            mPolygon.setPoints(points);
            mArea.points = points;
        }

        // called when a polygon was clicked
        // zooms onto the polygon and enables 'edit mode'
        public void focus(GoogleMap map) {
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Marker marker : mMarkers) {
                builder.include(marker.getPosition());
            }

            LatLng center = builder.build().getCenter();
            LatLng offset = new LatLng(center.latitude - 5.0, center.longitude);

            setMarkerVisible(true);
            map.animateCamera(CameraUpdateFactory.newLatLngZoom(offset, 4.0f), 500, null);
        }

        // called when an area is removed
        // removes all the markers and the polygon from the map
        public void delete() {
            mPolygon.remove();
            for (Marker marker : mMarkers) {
                marker.remove();
            }

            mMarkers = null;
            mPolygon = null;
            mArea = null;
        }

        public AreasViewModel.Area getArea() {
            return mArea;
        }
    }

    private AreasViewModel areasViewModel;
    private FragmentAreasBinding binding;

    private GoogleMap mMap = null;

    // hold a list of all shown polygons
    private final List<DraggablePolygon> mPolygons = new ArrayList<>();
    private DraggablePolygon mCurrentFocused = null;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        areasViewModel =
                new ViewModelProvider(this).get(AreasViewModel.class);

        binding = FragmentAreasBinding.inflate(inflater, container, false);
        binding.setViewModel(areasViewModel);

        binding.fab.setOnClickListener(view -> {
            if (mMap != null) {
                // add a new area in the center of the screen
                LatLng center = mMap.getProjection().getVisibleRegion().latLngBounds.getCenter();
                // TODO: change name
                areasViewModel.onAddArea("TestArea", center);
            }
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
            Log.d("main", "Polygon click");

            // search for clicked polygon object and focus on it
            for (DraggablePolygon dragPoly : mPolygons) {
                if (polygon.equals(dragPoly.mPolygon)) {
                    focusOn(dragPoly);
                    break;
                }
            }
        });

        // unfocus the current polygon
        mMap.setOnMapClickListener(latLng -> focusOn(null));

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
        areasViewModel.getAreas().observe(getViewLifecycleOwner(), areas -> {
            Log.d("main", "Areas changed");

            // handle removed areas
            List<DraggablePolygon> deletes = new ArrayList<>();
            for (DraggablePolygon dragPoly : mPolygons) {
                boolean found = false;

                for (AreasViewModel.Area area : areas) {
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
            for (AreasViewModel.Area area : areas) {
                if (findDragPoly(area) == null) {
                    DraggablePolygon dragPoly = new DraggablePolygon(mMap, area);
                    mPolygons.add(dragPoly);
                }
            }
        });
    }

    // helper function to find a DraggablePolygon with an Area
    private @Nullable DraggablePolygon findDragPoly(AreasViewModel.Area area) {
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
    private void focusOn(@Nullable DraggablePolygon dragPoly) {
        if (dragPoly != null) {
            if (mCurrentFocused != null) {
                mCurrentFocused.setMarkerVisible(false);
            }

            mCurrentFocused = dragPoly;
            dragPoly.focus(mMap);

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());
            binding.bottomSheet.setVisibility(View.VISIBLE);
            binding.fab.setVisibility(View.GONE);
        } else {
            if (mCurrentFocused != null) {
                mCurrentFocused.setMarkerVisible(false);
                mCurrentFocused = null;
            }

            mMap.animateCamera(CameraUpdateFactory.zoomOut());

            TransitionManager.beginDelayedTransition((ViewGroup) binding.getRoot());
            binding.bottomSheet.setVisibility(View.GONE);
            binding.fab.setVisibility(View.VISIBLE);
        }
    }

    // helper function to find and update a DraggablePolygon with a Marker
    private void updatePolygon(@NonNull Marker marker) {
        DraggablePolygon dragPoly = findDragPoly(marker);
        if (dragPoly != null) {
            dragPoly.updatePolygon();
        }
    }
}