package com.example.samsungproject.activities;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import com.example.samsungproject.R;
import com.example.samsungproject.databinding.ActivityMainBinding;
import com.example.samsungproject.databinding.FragmentSettingsBinding;
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
    ActivityMainBinding binding;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        binding = ActivityMainBinding.inflate(getLayoutInflater());
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
        setContentView(binding.getRoot());
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean isGuest = prefs.getBoolean("is_guest", false);
        boolean isLoggedIn = currentUser != null && !isGuest;
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
        binding.bottomNavigationView.setOnItemSelectedListener(item -> {
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

    public void setBottomNavigationVisibility(int visibility) {
        binding.bottomNavigationView.setVisibility(visibility);
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
            if (!menuFragment.isAdded()) {
                transaction.add(R.id.fragment_container, menuFragment, String.valueOf(R.id.menuFragment));
            }
            transaction.show(menuFragment).commit();
            activeFragment = menuFragment;
            binding.bottomNavigationView.setVisibility(View.VISIBLE);
            binding.bottomNavigationView.setSelectedItemId(R.id.menuFragment);
        }
    }
}