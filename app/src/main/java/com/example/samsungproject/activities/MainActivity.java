package com.example.samsungproject.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;

import com.example.samsungproject.R;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            NavController navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNavigationView, navController);
            bottomNavigationView.setOnItemSelectedListener(item -> {
                int itemId = item.getItemId();
                int currentDestinationId = navController.getCurrentDestination().getId();
                if (itemId == currentDestinationId) {

                    return true;
                }
                navController.navigate(itemId);

                return true;
            });
        } else {
            Log.e("NavHostFragment", "Ошибка: navHostFragment = null");
        }
    }
}