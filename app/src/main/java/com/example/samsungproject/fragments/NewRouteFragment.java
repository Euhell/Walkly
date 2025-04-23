package com.example.samsungproject.fragments;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.TextView;

import com.example.samsungproject.APICallback;
import com.example.samsungproject.API_IGNORE;
import com.example.samsungproject.DistanceType;
import com.example.samsungproject.POI;
import com.example.samsungproject.R;
import com.example.samsungproject.databinding.FragmentNewrouteBinding;
import com.example.samsungproject.fetchers.POIFetcher;
import com.example.samsungproject.fetchers.RouteFetcher;

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

public class NewRouteFragment extends Fragment {
    private final int REQUEST_PERMISSIONS_REQUEST_CODE = 1;
    private MapView map = null;
    private TextView distanceLeft;
    private ImageButton myLocation;
    private MyLocationNewOverlay myLocationOverlay;
    private RouteFetcher routeFetcher;
    private POIFetcher poiFetcher;
    private Polyline routeOverlay;
    private GeoPoint lastLocation = null;
    private long lastUpdateTime = 0;
    private static final long UPDATE_INTERVAL_MS = 180000; // 3 minutes
    private static final double MIN_DISTANCE_CHANGE_METERS = 50;
    private final DistanceType distancetype = MenuFragment.currentDistance;
    private static double endLon;
    private static double endLat;
    private boolean routeReady = false;
    private int poiRetryCount = 0;
    private ArrayList<POI> selectedTags;
    private FragmentNewrouteBinding binding;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentNewrouteBinding.inflate(inflater, container, false);
        Configuration.getInstance().load(requireContext(), PreferenceManager.getDefaultSharedPreferences(getContext()));
        distanceLeft = binding.distanceLeft;
        map = binding.map;
        map.setTileSource(TileSourceFactory.MAPNIK);
        map.setMultiTouchControls(true);
        requestPermissionsIfNecessary(new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_EXTERNAL_STORAGE});
        myLocationOverlay = new MyLocationNewOverlay(new GpsMyLocationProvider(requireContext()), map);
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        map.getController().setZoom(18.0);
        map.getTileProvider().clearTileCache();
        map.getOverlays().add(myLocationOverlay);
        myLocation = binding.myLocationButton;
        myLocation.setOnClickListener(v -> {
            Animation anim = AnimationUtils.loadAnimation(requireContext(), R.anim.scale_click);
            myLocation.startAnimation(anim);
            GeoPoint currentLocation = myLocationOverlay.getMyLocation();
            if (currentLocation != null) {
                map.getController().setZoom(18.0);
                map.getController().animateTo(currentLocation);
            }
        });
        if (routeFetcher == null) {
            routeFetcher = new RouteFetcher();
        }
        if (poiFetcher == null) {
            poiFetcher = new POIFetcher();
        }
        if (distancetype != null && getArguments() != null) {
            selectedTags = (ArrayList<POI>) getArguments().getSerializable("selectedTags");
            generatePath();
        }

        return binding.getRoot();
    }

    @Override
    public void onResume() {
        super.onResume();
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.enableFollowLocation();
        if (routeReady) {
            updateLocation();
        }
        map.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        map.onPause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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
            ActivityCompat.requestPermissions(requireActivity(),
                    permissionsToRequest.toArray(new String[0]),
                    REQUEST_PERMISSIONS_REQUEST_CODE);
        }
    }

    private void drawRoute(double startLat, double startLon, double endLat, double endLon,
                           String profile) throws IOException {
        routeFetcher.fetchRoute(startLat, startLon, endLat, endLon, new APICallback() {
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

    private void generatePath() {
        myLocationOverlay.enableMyLocation();
        myLocationOverlay.runOnFirstFix(() -> requireActivity().runOnUiThread(() -> {
        GeoPoint startLocation = myLocationOverlay.getMyLocation();
        if (startLocation == null) {
            Log.e("generatePath", "Не удалось получить текущее местоположение");
            return;
        }
        Log.d("PathGenerator", "Стартовые координаты: " + startLocation.getLatitude() + ", " + startLocation.getLongitude());
        GeoPoint endLatLon = getRandomGeoPoint(distancetype, startLocation);
        poiFetcher.getNearestPOI(endLatLon, selectedTags,
                new APICallback() {
                    @Override
                    public void onSuccess(List<double[]> routePoints) {
                        if (routePoints.isEmpty()) {
                            Log.e("POIFetcher", "Пустой список POI");
                            return;
                        }
                        requireActivity().runOnUiThread(() -> {
                            endLat = routePoints.get(0)[0];
                            endLon = routePoints.get(0)[1];
                            Log.d("PathGenerator(POIFetcher)", "Конечные координаты: " + endLat + ", " + endLon);
                            double distance = distanceBetween(startLocation, new GeoPoint(endLat, endLon));
                            distanceLeft.setText(formatDistance(distance));
                            routeReady = true;
                            updateLocation();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (error.equals(API_IGNORE.ERROR_NO_POI)) {
                            if (poiRetryCount < API_IGNORE.MAX_POI_RETRIES) {
                                poiRetryCount++;
                                Log.w("POIFetcher", "POI не найден, попытка " + poiRetryCount);
                                requireActivity().runOnUiThread(() -> generatePath());
                            } else {
                                Log.e("POIFetcher", "Максимальное количество попыток достигнуто");
                            }
                        } else {
                            Log.e("PathGenerator(POIFetcher)", error);
                        }
                    }
                });
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
        double distance = distanceBetween(lastLocation, newLocation);
        distanceLeft.setText(formatDistance(distance));

        return distance > MIN_DISTANCE_CHANGE_METERS;
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
        // return (int)p1.distanceToAsDouble(p2);

    }

    private GeoPoint getRandomGeoPoint(DistanceType distanceType, GeoPoint startLocation) {
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
        double angle = random.nextDouble() * 2 * Math.PI; // 111000м ≈ 1° широты
        double deltaLat = startLocation.getLatitude() + (distance / 111000) * Math.cos(angle);
        double deltaLon = startLocation.getLongitude() + (distance / (111000 * Math.cos(
                Math.toRadians(startLocation.getLatitude())))) * Math.sin(angle);
        return new GeoPoint(deltaLat, deltaLon);
    }

    public static NewRouteFragment newInstance(ArrayList<POI> selectedTags) {
        NewRouteFragment fragment = new NewRouteFragment();
        Bundle args = new Bundle();
        args.putSerializable("selectedTags", selectedTags);
        fragment.setArguments(args);
        return fragment;
    }

    private String getSelectedUnit() {
        return PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("unit_pref", "Метры");
    }

    private String formatDistance(double meters) {
        String unit = getSelectedUnit();
        switch (unit) {
            case "Мили":
                return String.format("%.2f миль", meters / 1609.34);
            case "Футы":
                return String.format("%.2f футов", meters / 0.3048);
            case "Ярды":
                return String.format("%.2f ярдов", meters / 0.9144);
            case "Метры":
                return meters >= 1000 ? String.format("%.2f км", meters / 1000)
                        : String.format("%.0f м", meters);
            default:
                throw new IllegalStateException("Unexpected value: " + unit);
        }
    }

}