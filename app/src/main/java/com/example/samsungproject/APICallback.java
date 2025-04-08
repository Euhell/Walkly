package com.example.samsungproject;

import java.util.List;

public interface APICallback {
    void onSuccess(List<double[]> routePoints);
    void onError(String error);
}