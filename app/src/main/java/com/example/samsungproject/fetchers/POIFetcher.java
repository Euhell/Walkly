package com.example.samsungproject.fetchers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.samsungproject.APICallback;

import org.json.JSONArray;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class POIFetcher {

    private static final String API_URL = "https://overpass-api.de/api/interpreter?data=[out:json];";
    private OkHttpClient client = new OkHttpClient();

    public GeoPoint getNearestPOI(double lat, double lon, String profile,
                                  APICallback callback) {
        String url = API_URL + "node(around:500," + lat + "," + lon + ")[\"" + profile + "\"];out;";
        Request request = new Request.Builder().url(url).build();
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
                // Log.d("POIResponse", responseBody);
                try {
                    JSONObject jsonResponse = new JSONObject(responseBody);
                    JSONArray elements = jsonResponse.getJSONArray("elements");
                    if (elements.length() == 0) {
                        callback.onError("Нет POI поблизости");
                        return;
                    }
                    JSONObject poi = elements.getJSONObject(new Random().nextInt(elements.length()));
                    double poiLat = poi.getDouble("lat");
                    double poiLon = poi.getDouble("lon");
                    JSONObject tags = poi.optJSONObject("tags");
                    String name = tags != null ? tags.optString("name", "Без названия") : "Без названия";
                    String type = tags != null ? tags.toString() : "нет тегов";
                    Log.d("POIFetcher", "Выбранный POI: " + name + " (" + poiLat + ", " + poiLon + ")");
                    Log.d("POIFetcher", "Все теги POI: " + type);
                    List<double[]> points = new ArrayList<>();
                    points.add(new double[]{poiLat, poiLon});
                    callback.onSuccess(points);
                } catch (Exception e) {
                    callback.onError("Ошибка парсинга JSON: " + e.getMessage());
                    Log.e("JSONParseError", "Ошибка парсинга: " + e.getMessage());
                }
            }
        });
        return null;
    }
}

//    public int getRandomPOIType(String[] poiTags) {
//
//    }