package com.example.samsungproject.fetchers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.samsungproject.APICallback;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RouteFetcher {
    private static final String API_URL = "https://graphhopper.com/api/1/route";
    private final String API_KEY = "c7324357-5b93-4dcc-b43f-241565845842";
    private OkHttpClient client = new OkHttpClient();

    public void fetchRoute(double startLat, double startLon, double endLat, double endLon,
                           APICallback callback, String profile) throws IOException {
        String url = API_URL + "?point=" + startLat + "," + startLon + "&point=" + endLat + "," + endLon +
                "&profile=" + profile + "&locale=ru&key=" + API_KEY + "&points_encoded=false&avoid=motorway,trunk";
        Request request = new Request.Builder().url(url).get().build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                callback.onError(e.getMessage());
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError("Ошибка: " + response.code());
                    return;
                }
                String responseBody = response.body().string();
                // Log.d("RouteResponse", responseBody);
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray paths = jsonResponse.getJSONArray("paths");
                    if (paths.length() == 0) {
                        callback.onError("Маршрут не найден");
                        return;
                    }
                    JSONObject pointsObject = paths.getJSONObject(0).getJSONObject("points");
                    JSONArray coordinates = pointsObject.getJSONArray("coordinates");
                    List<double[]> routePoints = new ArrayList<>();
                    for (int i = 0; i < coordinates.length(); i++) {
                        JSONArray point = coordinates.getJSONArray(i);
                        routePoints.add(new double[]{point.getDouble(1), point.getDouble(0)});
                    }
                    callback.onSuccess(routePoints);
                } catch (Exception e) {
                    callback.onError("Ошибка парсинга JSON: " + e.getMessage());
                    Log.e("JSONParseError", "Ошибка парсинга: " + e.getMessage());
                }
            }
        });
    }
}