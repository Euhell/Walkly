package com.example.samsungproject;

import org.chromium.base.Callback;

import java.io.IOException;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RouteFetcher {
    private static final String API_URL = "https://graphhopper.com/api/1/route";
    private final String API_KEY = "c7324357-5b93-4dcc-b43f-241565845842";
    OkHttpClient client = new OkHttpClient();
    public void fetchRoute(double startLat, double startLon, double endLat, double endLon, Callback callback) {
        Request request = new Request.Builder()
                .addHeader("point", startLat + "," + startLon)
                .addHeader("point", endLat + "," + endLon)
                .addHeader("profile", "foot") // or car or bike
                .addHeader("locale", "ru")
                .addHeader("key", API_KEY)
                .build();

        try {
            Response response = client.newCall(request).execute();
        } catch (
                IOException e) {
            throw new RuntimeException(e);
        }
    }
}
