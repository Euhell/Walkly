package com.example.samsungproject;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import org.osmdroid.views.MapView;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.views.overlay.mylocation.GpsMyLocationProvider;
import org.osmdroid.views.overlay.mylocation.MyLocationNewOverlay;

import java.io.IOException;
import java.util.ArrayList;

import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.overlay.Polyline;

import java.util.List;
import java.util.Random;

public class NewpathFragment extends Fragment {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private TextView distanceLeft;
    private MyLocationNewOverlay myLocationOverlay;
    private RouteFetcher routeFetcher;
    private Polyline routeOverlay;
    private GeoPoint lastLocation = null;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 180000; // 3 minutes
    private static final double MIN_DISTANCE_CHANGE_METERS = 50;
    private final MenuFragment.DistanceType distancetype = MenuFragment.currentDistance;
    private static double endLon;
    private static double endLat;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_newpath, container, false);
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        map = view.findViewById(R.id.map);
        distanceLeft = view.findViewById(R.id.distanceLeft);
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
        });
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        map.getController().setZoom(18.0);
        map.getTileProvider().clearTileCache();
        map.getOverlays().add(myLocationOverlay);
        routeFetcher = new RouteFetcher();
        generatePath(distancetype); // generate the end of the path
        // endLat = 55.7998; метро Аэропорт (для тестов)
        // endLon = 37.5341;
        updateLocation(); // initialize route & location
        ImageButton imgbtn = view.findViewById(R.id.imageButton);
        imgbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Navigation.findNavController(view).navigate(R.id.action_newpathFragment_to_menuFragment);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        updateLocation();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));
        map.onResume(); //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onPause() {
        super.onPause();
        //this will refresh the osmdroid configuration on resuming.
        //if you make changes to the configuration, use
        //SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //Configuration.getInstance().save(this, prefs);
        map.onPause();  //needed for compass, my location overlays, v6.0.0 and up
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, int[] grantResults) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (int i = 0; i < grantResults.length; i++) {
            permissionsToRequest.add(permissions[i]);
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            myLocationOverlay.enableMyLocation();
            myLocationOverlay.enableFollowLocation();
        } else {
            Log.e("Permissions", "Разрешение на геолокацию не выдано");
        }
    }

    private void requestPermissionsIfNecessary(String[] permissions) {
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String permission : permissions) {
            if (ContextCompat.checkSelfPermission(requireContext(), permission)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(permission);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void drawRoute(double startLat, double startLon, double endLat, double endLon,
                           String profile) throws IOException {
        routeFetcher.fetchRoute(startLat, startLon, endLat, endLon, new RouteFetcher.RouteCallback() {
            @Override
            public void onSuccess(List<double[]> routePoints) {
                if (routeOverlay != null) {
                    map.getOverlays().remove(routeOverlay);
                }
                routeOverlay = new Polyline();
                List<GeoPoint> geoPoints = new ArrayList<>();
                for (double[] point : routePoints) {
                    geoPoints.add(new GeoPoint(point[0], point[1]));
                }
                routeOverlay.setPoints(geoPoints);
                routeOverlay.setColor(Color.rgb(153, 50, 204));
                routeOverlay.setGeodesic(true);
                map.getOverlays().add(routeOverlay);
                map.invalidate();
                Log.d("RouteDebug", "Маршрут добавлен: " + routeOverlay.getPoints().size() + " точек");
            }

            @Override
            public void onError(String error) {
                Log.e("RouteError", error);
            }
        }, profile);
    }

    private void generatePath(MenuFragment.DistanceType distanceType) {
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(() -> requireActivity().runOnUiThread(() -> {
        GeoPoint startLocation = myLocationOverlay.getMyLocation();
        if (startLocation == null) {
            Log.e("generatePath", "Не удалось получить текущее местоположение");
            return;
        }
        Log.d("PathGenerator", "Стартовые координаты: " + startLocation.getLatitude() + ", " + startLocation.getLongitude());
        Random random = new Random();
        double distance;
        switch (distanceType) {
            case Small:
                distance = 500 + 500 * random.nextDouble();
                break;
            case Medium:
                distance = 1000 + 1000 * random.nextDouble();
                break;
            case Long:
                distance = 1500 + 1500 * random.nextDouble();
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + distanceType);
        }
        double angle = random.nextDouble() * 2 * Math.PI;
        double deltaLat = (distance / 111000) * Math.cos(angle); // 111000м ≈ 1° широты
        double deltaLon = (distance / (111000 * Math.cos(Math.toRadians(startLocation.getLatitude())))) * Math.sin(angle);
        endLat = startLocation.getLatitude() + deltaLat;
        endLon = startLocation.getLongitude() + deltaLon;
        Log.d("PathGenerator", "Конечные координаты: " + endLat + ", " + endLon);
        }));
    }

    private void updateLocation() {
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(() -> requireActivity().runOnUiThread(() -> {
            GeoPoint currentLocation = myLocationOverlay.getMyLocation();
            if (currentLocation != null) {
                long currentTime = System.currentTimeMillis();
                if (shouldUpdateRoute(currentLocation, currentTime)) {
                    Log.d("UpdateLocation", "Обновление маршрута: " +
                            currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                    try {
                        drawRoute(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                endLat, endLon, "foot");
                        lastLocation = currentLocation;
                        lastUpdateTime = currentTime;
                    } catch (IOException e) {
                        Log.e("RouteError", "Ошибка построения маршрута", e);
                    }
                } else {
                    Log.d("UpdateLocation", "Обновление маршрута не требуется");
                }
            } else {
                Log.e("UpdateLocation", "Не удалось получить местоположение");
            }
        }));
    }

    private boolean shouldUpdateRoute(GeoPoint newLocation, long currentTime) {
        if (lastLocation == null) {
            return true;
        }
        if ((currentTime - lastUpdateTime) < UPDATE_INTERVAL_MS) {
            return false;
        }
        double dist = distanceBetween(lastLocation, newLocation);
        distanceLeft.setText((int) dist);
        Log.d("DIST", String.valueOf((int)dist));
        return dist > MIN_DISTANCE_CHANGE_METERS;
    }

    private double distanceBetween(GeoPoint p1, GeoPoint p2) {
        double lat1 = p1.getLatitude();
        double lon1 = p1.getLongitude();
        double lat2 = p2.getLatitude();
        double lon2 = p2.getLongitude();
        double earthRadius = 6371000;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        // Haversine formula
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return earthRadius * c;
    }
}