package com.example.samsungproject.fragments;

import android.app.Dialog;
import android.os.Bundle;

import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samsungproject.R;

public class MenuFragment extends Fragment {
    public enum DistanceType {
        Small,
        Medium,
        Long
    }
    public static DistanceType currentDistance;
    String[] items = {"Длинный", "Средний", "Короткий"};
    AutoCompleteTextView autoCompleteTextView;
    ArrayAdapter<String> adapterItems;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_menu, container, false);
        Button btn = view.findViewById(R.id.button);
        btn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showDialog();
            }
        });

        return view;
    }

    private void showDialog() {
        currentDistance = null;
        Dialog dialog = new Dialog(requireContext(), R.style.DialogStyle);
        dialog.setContentView(R.layout.dialog_layout);
        View layout = getLayoutInflater().inflate(R.layout.toast_layout, null);
        Toast toast = new Toast(MenuFragment.this.requireContext());
        toast.setDuration(Toast.LENGTH_SHORT);
        toast.setView(layout);
        TextView toastText = layout.findViewById(R.id.toast_text);
        autoCompleteTextView = dialog.findViewById(R.id.auto_complete_text);
        adapterItems = new ArrayAdapter<>(requireContext(), R.layout.list_layout, items);
        autoCompleteTextView.setAdapter(adapterItems);

        autoCompleteTextView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String item = parent.getItemAtPosition(position).toString();
                autoCompleteTextView.setText(item, false);
                if (item.equals(items[0])) {
                    currentDistance = DistanceType.Long;
                } else if (item.equals(items[1])) {
                    currentDistance = DistanceType.Medium;
                } else {
                    currentDistance = DistanceType.Small;
                }
                toastText.setText("Выбрано: " + item);
                toast.show();
            }
        });
        autoCompleteTextView.setOnClickListener(v -> autoCompleteTextView.showDropDown());
        Button dlgbtn = dialog.findViewById(R.id.button5);
        dlgbtn.setOnClickListener(v -> {
            if (currentDistance != null) {
                dialog.dismiss();
                Navigation.findNavController(requireActivity(), R.id.nav_host_fragment)
                        .navigate(R.id.action_menuFragment_to_newpathFragment);
            } else {
                toastText.setText("Ничего не выбрано");
                toast.show();
            }
        });
        dialog.show();
    }
}