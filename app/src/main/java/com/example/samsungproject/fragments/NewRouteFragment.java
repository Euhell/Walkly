package com.example.samsungproject.fragments;

import android.Manifest;
import android.content.SharedPreferences;
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
import android.widget.Toast;

import com.example.samsungproject.APICallback;
import com.example.samsungproject.types.DistanceType;
import com.example.samsungproject.types.POI;
import com.example.samsungproject.R;
import com.example.samsungproject.databinding.FragmentNewrouteBinding;
import com.example.samsungproject.fetchers.POIFetcher;
import com.example.samsungproject.fetchers.RouteFetcher;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

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
    private MapView map;
    private TextView distanceLeft;
    private ImageButton myLocation;
    private MyLocationNewOverlay myLocationOverlay;
    private RouteFetcher routeFetcher;
    private POIFetcher poiFetcher;
    private Polyline routeOverlay;
    private GeoPoint lastLocation;
    private GeoPoint endLocation;
    private static final double MIN_DISTANCE_CHANGE_METERS = 15;
    private static final int MAX_POI_RETRIES = 3;
    private final DistanceType distancetype = MenuFragment.currentDistance;
    private static double endLon;
    private static double endLat;
    private boolean routeReady = false;
    private boolean routeCompleted = false;
    private long routeDistance;
    private SharedPreferences prefs;
    private int poiRetryCount = 0;
    private ArrayList<POI> selectedTags;
    private FragmentNewrouteBinding binding;


    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
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
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
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
                            endLocation = new GeoPoint(endLat, endLon);
                            double distance = distanceBetween(startLocation, endLocation);
                            routeDistance = (long) distance;
                            distanceLeft.setText(formatDistance(distance));
                            routeReady = true;
                            routeCompleted = false;
                            updateLocation();
                        });
                    }

                    @Override
                    public void onError(String error) {
                        if (error.equals(POIFetcher.ERROR_NO_POI)) {
                            if (poiRetryCount < MAX_POI_RETRIES) {
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
                if (shouldUpdateRoute(currentLocation)) {
                    Log.d("UpdateLocation", "Обновление маршрута: " +
                            currentLocation.getLatitude() + ", " + currentLocation.getLongitude());
                    try {
                        drawRoute(currentLocation.getLatitude(), currentLocation.getLongitude(),
                                endLat, endLon, "foot");
                        lastLocation = currentLocation;
                    } catch (IOException e) {
                        Log.e("RouteError", "Ошибка построения маршрута", e);
                    }
                } else if (distanceBetween(currentLocation, endLocation) <= 10) {
                    distanceLeft.setText("Маршрут пройден");
                    routeCompleted = true;
                    String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                            ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                            : "guest";
                    String distanceKey = "total_distance_" + userId;
                    long totalDistance = prefs.getLong(distanceKey, 0);
                    prefs.edit().putLong(distanceKey, totalDistance + routeDistance).apply();
                    FirebaseFirestore.getInstance()
                            .collection("users")
                            .document(FirebaseAuth.getInstance().getUid())
                            .update("score", totalDistance + routeDistance);
                    Toast.makeText(requireContext(), "Вы достигли точки назначения!", Toast.LENGTH_LONG).show();
                    map.getOverlays().remove(routeOverlay);
                } else {
                    Log.d("UpdateLocation", "Обновление маршрута не требуется");
                }
            } else {
                Log.e("UpdateLocation", "Не удалось получить местоположение");
            }
        }));
    }

    private boolean shouldUpdateRoute(GeoPoint newLocation) {
        if (lastLocation == null) {
            return true;
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


    private String formatDistance(double meters) {
        String unit = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("unit_pref", "Метры");
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