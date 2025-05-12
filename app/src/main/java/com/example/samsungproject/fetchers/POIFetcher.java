package com.example.samsungproject.fetchers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.samsungproject.APICallback;
import com.example.samsungproject.types.POI;

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

    private static final String API_URL = "https://overpass.kumi.systems/api/interpreter?data=[out:json][timeout:25];(";
    private OkHttpClient client = new OkHttpClient();
    public static final String ERROR_NO_POI = "NO_POI_FOUND";

    public void getNearestPOI(GeoPoint point, ArrayList<POI> selectedTags,
                              APICallback callback) {
        StringBuilder query = new StringBuilder(API_URL);
        for (POI poi : selectedTags) {
            query.append("node(around:500,")
                    .append(point.getLatitude()).append(",")
                    .append(point.getLongitude()).append(")")
                    .append(poi.getTag()).append(";");
        }
        query.append(");out body;");
        String url = query.toString();
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
                        callback.onError(ERROR_NO_POI);
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
    }
}