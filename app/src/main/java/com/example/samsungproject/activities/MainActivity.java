package com.example.samsungproject.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.samsungproject.R;
import com.example.samsungproject.fragments.LoginFragment;
import com.example.samsungproject.fragments.MenuFragment;
import com.example.samsungproject.fragments.NewRouteFragment;
import com.example.samsungproject.fragments.SettingsFragment;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements LoginFragment.OnLoginSuccessListener {
    private final Map<Integer, Fragment> fragmentMap = new HashMap<>();
    private Fragment activeFragment;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String theme = prefs.getString("app_theme", "Системная");
        int mode;
        if ("Светлая".equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_NO;
        } else if ("Тёмная".equals(theme)) {
            mode = AppCompatDelegate.MODE_NIGHT_YES;
        } else {
            mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
        }
        AppCompatDelegate.setDefaultNightMode(mode);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isLoggedIn = currentUser != null;
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
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            if (isLoggedIn) {
                activeFragment = fragmentMap.get(R.id.menuFragment);
                transaction.show(activeFragment);
            } else {
                LoginFragment loginFragment = new LoginFragment();
                transaction.add(R.id.fragment_container, loginFragment, "login");
                activeFragment = loginFragment;
            }
            transaction.commit();
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

    @Override
    public void onLoginSuccess() {
        FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
        Fragment loginFragment = getSupportFragmentManager().findFragmentByTag("login");
        if (loginFragment != null) {
            transaction.remove(loginFragment);
        }
        Fragment menuFragment = fragmentMap.get(R.id.menuFragment);
        if (menuFragment != null) {
            transaction.show(menuFragment).commit();
            activeFragment = menuFragment;
        }
    }
}