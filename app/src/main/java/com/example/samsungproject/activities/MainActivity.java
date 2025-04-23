package com.example.samsungproject.activities;

import android.os.Bundle;
import android.util.Log;

import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.samsungproject.R;
import com.example.samsungproject.fragments.MenuFragment;
import com.example.samsungproject.fragments.NewRouteFragment;
import com.example.samsungproject.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();
    private Fragment activeFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);
        fragmentMap.put(R.id.menuFragment, new MenuFragment());
        fragmentMap.put(R.id.newpathFragment, new NewRouteFragment());
        fragmentMap.put(R.id.settingsFragment, new SettingsFragment());
        if (savedInstanceState == null) {
            for (Map.Entry<Integer, Fragment> entry : fragmentMap.entrySet()) {
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.fragment_container, entry.getValue(), String.valueOf(entry.getKey()))
                        .hide(entry.getValue())
                        .commit();
            }
            activeFragment = fragmentMap.get(R.id.menuFragment);
            getSupportFragmentManager().beginTransaction().show(activeFragment).commit();
        } else {
            for (Map.Entry<Integer, Fragment> entry : fragmentMap.entrySet()) {
                Fragment fragment = getSupportFragmentManager().findFragmentByTag(String.valueOf(entry.getKey()));
                if (fragment != null) fragmentMap.put(entry.getKey(), fragment);
            }
            activeFragment = getSupportFragmentManager().getFragments().stream()
                    .filter(Fragment::isVisible).findFirst()
                    .orElse(fragmentMap.get(R.id.menuFragment));
        }
        bottomNavigationView.setOnItemSelectedListener(item -> {
            Fragment selectedFragment = fragmentMap.get(item.getItemId());
            if (selectedFragment != null && selectedFragment != activeFragment) {
                FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
                for (Fragment fragment : fragmentMap.values()) {
                    if (fragment.isAdded() && fragment != selectedFragment) {
                        transaction.hide(fragment);
                    }
                }
                if (!selectedFragment.isAdded()) {
                    transaction.add(R.id.fragment_container, selectedFragment, String.valueOf(item.getItemId()));
                } else {
                    transaction.show(selectedFragment);
                }
                transaction.commit();
                activeFragment = selectedFragment;
            }
            return true;
        });
    }

    public void updateFragment(int id, Fragment newFragment) {
        fragmentMap.put(id, newFragment);
        activeFragment = newFragment;
    }
}