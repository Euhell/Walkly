package com.example.samsungproject.fragments;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import com.example.samsungproject.databinding.FragmentSettingsBinding;

import java.util.Arrays;

public class SettingsFragment extends Fragment {
    private FragmentSettingsBinding binding;
    private SharedPreferences prefs;
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        String[] units = {"Метры", "Мили", "Футы", "Ярды"};
        String[] themes = {"Системная", "Светлая", "Тёмная"};
        ArrayAdapter<String> adapterUnits = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_list_item_1, units);
        ArrayAdapter<String> adapterTheme = new ArrayAdapter<>(requireContext(),
                android.R.layout.simple_spinner_dropdown_item, themes);
        binding.units.setAdapter(adapterUnits);
        binding.theme.setAdapter(adapterTheme);
        prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String currentUnits = prefs.getString("unit_pref", "Метры");
        String currentTheme = prefs.getString("app_theme", "Системная");
        binding.units.setSelection(Arrays.asList(units).indexOf(currentUnits));
        final boolean[] unitInitialized = {false};
        binding.units.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!unitInitialized[0]) {
                    unitInitialized[0] = true;
                    return;
                }
                String selectedUnit = parent.getItemAtPosition(position).toString();
                String oldUnit = prefs.getString("unit_pref", "Метры");
                if (!selectedUnit.equals(oldUnit)) {
                    prefs.edit().putString("unit_pref", selectedUnit).apply();
                    Toast.makeText(requireContext(), "Выбрано: " + selectedUnit, Toast.LENGTH_SHORT).show();
                    requireActivity().recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
        binding.theme.setSelection(Arrays.asList(themes).indexOf(currentTheme));
        final boolean[] themeInitialized = {false};
        binding.theme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!themeInitialized[0]) {
                    themeInitialized[0] = true;
                    return;
                }
                String selectedTheme = parent.getItemAtPosition(position).toString();
                String oldTheme = prefs.getString("app_theme", "Системная");
                if (!selectedTheme.equals(oldTheme)) {
                    prefs.edit().putString("app_theme", selectedTheme).apply();
                    int mode;
                    if (selectedTheme.equals("Светлая")) {
                        mode = AppCompatDelegate.MODE_NIGHT_NO;
                    } else if (selectedTheme.equals("Тёмная")) {
                        mode = AppCompatDelegate.MODE_NIGHT_YES;
                    } else {
                        mode = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM;
                    }
                    Toast.makeText(requireContext(), "Выбрано: " + mode, Toast.LENGTH_SHORT).show();
                    AppCompatDelegate.setDefaultNightMode(mode);
                    requireActivity().recreate();
                }
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}