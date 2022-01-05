package de.mytrack.mytrackapp.ui.statistics.views;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.ArrayList;
import java.util.List;

import de.mytrack.mytrackapp.R;
import de.mytrack.mytrackapp.data.Area;
import de.mytrack.mytrackapp.data.AreaPoint;
import de.mytrack.mytrackapp.databinding.FragmentMapViewBinding;

public class MapViewFragment extends Fragment implements OnMapReadyCallback {

    private MapViewViewModel mapViewViewModel;
    private FragmentMapViewBinding binding;

    private GoogleMap mMap = null;

    private final List<Polygon> mPolygons = new ArrayList<>();
    private final List<Polyline> mPolylines = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mapViewViewModel = new ViewModelProvider(this).get(MapViewViewModel.class);

        binding = FragmentMapViewBinding.inflate(inflater, container, false);
        binding.setLifecycleOwner(getViewLifecycleOwner());
        binding.setViewModel(mapViewViewModel);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        return binding.getRoot();
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        this.mMap = googleMap;

        mapViewViewModel.getAreaConnectionsData().observe(getViewLifecycleOwner(), areaConnections -> {
            for (Polygon polygon : mPolygons) {
                polygon.remove();
            }

            for (Polyline polyline : mPolylines) {
                polyline.remove();
            }

            if (areaConnections.mAreas.isEmpty())
                return;

            // create a polygon for every area in areaSet
            for (Area area : areaConnections.mAreas) {
                int color = area.color;
                int fillColor = Color.argb(128, Color.red(color), Color.green(color),
                        Color.blue(color));

                PolygonOptions polygonOptions = new PolygonOptions()
                        .geodesic(true)
                        .fillColor(fillColor)
                        .strokeColor(color);

                for (AreaPoint point : area.points) {
                    LatLng corner = new LatLng(point.latitude, point.longitude);
                    polygonOptions.add(corner);
                }

                mPolygons.add(mMap.addPolygon(polygonOptions));
            }

            // connect areas with lines
            // TODO: improve line appearance
            // maybe add more points to approach a curve shape
            for (Pair<Area, Area> pair : areaConnections.mConnections) {
                LatLngBounds.Builder builder1 = new LatLngBounds.Builder();
                for (AreaPoint point : pair.first.points) {
                    LatLng latLng = new LatLng(point.latitude, point.longitude);
                    builder1.include(latLng);
                }

                LatLngBounds.Builder builder2 = new LatLngBounds.Builder();
                for (AreaPoint point : pair.second.points) {
                    LatLng latLng = new LatLng(point.latitude, point.longitude);
                    builder2.include(latLng);
                }

                PolylineOptions polylineOptions = new PolylineOptions()
                        .geodesic(true)
                        .color(0xff28719a)
                        .add(builder1.build().getCenter())
                        .add(builder2.build().getCenter());

                mPolylines.add(mMap.addPolyline(polylineOptions));
            }

            // create bounds that include every area
            LatLngBounds.Builder builder = new LatLngBounds.Builder();
            for (Area area : areaConnections.mAreas) {
                for (AreaPoint point : area.points) {
                    LatLng latLng = new LatLng(point.latitude, point.longitude);
                    builder.include(latLng);
                }
            }

            // animate the camera to include all areas
            mMap.animateCamera(CameraUpdateFactory.newLatLngBounds(builder.build(), 150), 500, null);
        });
    }
}
