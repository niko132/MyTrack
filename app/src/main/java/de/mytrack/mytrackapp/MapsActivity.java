package de.mytrack.mytrackapp;

import android.Manifest;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;

import androidx.activity.result.contract.ActivityResultContracts;
import androidx.fragment.app.FragmentActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        BottomNavigationView bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setOnItemSelectedListener(item -> {
            if (item.getItemId() == R.id.action_statistics) {

            } else if (item.getItemId() == R.id.action_activities) {

            } else if (item.getItemId() == R.id.action_areas) {

            } else if (item.getItemId() == R.id.action_settings) {

            }

            return true;
        });

        requestLocationPermission();
    }

    private void requestLocationPermission() {
        registerForActivityResult(new ActivityResultContracts.
                RequestMultiplePermissions(), result -> {
            boolean fineLocationGranted =
                    (result.containsKey(Manifest.permission.ACCESS_FINE_LOCATION) &&
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION) != null) ?
                            result.get(Manifest.permission.ACCESS_FINE_LOCATION) : false;

            boolean coarseLocationGranted =
                    (result.containsKey(Manifest.permission.ACCESS_COARSE_LOCATION) &&
                            result.get(Manifest.permission.ACCESS_COARSE_LOCATION) != null) ?
                            result.get(Manifest.permission.ACCESS_COARSE_LOCATION) : false;

            if (fineLocationGranted || coarseLocationGranted) {
                Log.d("main", "Location Granted");

                // start service only when we have the location permission
                startLocationService();
            } else {
                Log.d("main", "No Location Granted");
            }
        }).launch(new String[] {
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        });
    }

    private void startLocationService() {
        Intent locationServiceIntent = new Intent(this, LocationService.class);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(locationServiceIntent);
        } else {
            startService(locationServiceIntent);
        }
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));
    }
}