package com.example.samsungproject.fragments;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.example.samsungproject.types.DistanceType;
import com.example.samsungproject.adapters.LeaderboardAdapter;
import com.example.samsungproject.types.POI;
import com.example.samsungproject.adapters.POIAdapter;
import com.example.samsungproject.R;
import com.example.samsungproject.types.LeaderboardUser;
import com.example.samsungproject.activities.MainActivity;
import com.example.samsungproject.databinding.FragmentMenuBinding;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MenuFragment extends Fragment {
    private FragmentMenuBinding binding;
    public static DistanceType currentDistance;
    private String[] lengthItems;
    private AutoCompleteTextView autoCompleteTextView;
    private Bundle args;
    private ArrayList<POI> selectedTags;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        binding = FragmentMenuBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        if (getActivity() instanceof MainActivity) {
            MainActivity mainActivity = (MainActivity) getActivity();
            mainActivity.setBottomNavigationVisibility(View.VISIBLE);
        }
        args = new Bundle();
        selectedTags = new ArrayList<>();
        lengthItems = getResources().getStringArray(R.array.lengthItems);
        binding.button.setOnClickListener(v -> showDialog());
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String userId = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "guest";
        String distanceKey = "total_distance_" + userId;
        long totalDistance = prefs.getLong(distanceKey, 0);
        String SavedUnits = PreferenceManager.getDefaultSharedPreferences(requireContext())
                .getString("unit_pref", "Метры");
        switch (SavedUnits) {
            case "Мили":
                binding.distancePassed.setText("Всего пройдено: " +
                        String.format("%.2f миль", (double) totalDistance / 1609.34));
                break;
            case "Футы":
                binding.distancePassed.setText("Всего пройдено: " +
                        String.format("%.2f футов", (double) totalDistance / 0.3048));
                break;
            case "Ярды":
                binding.distancePassed.setText("Всего пройдено: " +
                        String.format("%.2f ярдов", (double) totalDistance / 0.9144));
                break;
            case "Метры":
                if (totalDistance >= 1000) {
                    binding.distancePassed.setText("Всего пройдено: " +
                            String.format("%.2f км", (double) totalDistance / 1000));
                } else {
                    binding.distancePassed.setText("Всего пройдено: " +
                            String.format("%.0f м", (double) totalDistance));
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + SavedUnits);
        }
        RecyclerView leaderboardRecycler = binding.leaderboard;
        leaderboardRecycler.setLayoutManager(new LinearLayoutManager(requireContext()));
        LeaderboardAdapter leaderboardAdapter = new LeaderboardAdapter(requireContext());
        leaderboardRecycler.setAdapter(leaderboardAdapter);
        FirebaseFirestore.getInstance()
                .collection("users")
                .orderBy("score", Query.Direction.DESCENDING)
                .limit(10)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<LeaderboardUser> userList = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        LeaderboardUser user = doc.toObject(LeaderboardUser.class);
                        userList.add(user);
                    }
                    leaderboardAdapter.setItems(userList);
                });
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
        RecyclerView recyclerView = dialog.findViewById(R.id.poi_recycler);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        List<POI> tagList = Arrays.asList(
                new POI("shop", null, "Магазины"),
                new POI("tourism", null, "Достопримечательность"),
                new POI("historic", null, "Памятник"));
        POIAdapter adapter = new POIAdapter(tagList, selectedTags);
        recyclerView.setAdapter(adapter);
        ArrayAdapter<String> adapterItems = new ArrayAdapter<>(requireContext(), R.layout.list_layout, lengthItems);
        autoCompleteTextView.setAdapter(adapterItems);
        autoCompleteTextView.setOnItemClickListener((parent, view, position, id) -> {
            String item = parent.getItemAtPosition(position).toString();
            autoCompleteTextView.setText(item, false);
            if (item.equals(lengthItems[0])) {
                currentDistance = DistanceType.Long;
            } else if (item.equals(lengthItems[1])) {
                currentDistance = DistanceType.Medium;
            } else {
                currentDistance = DistanceType.Small;
            }
            toastText.setText("Выбрано: " + item);
            toast.show();
        });
        autoCompleteTextView.setOnClickListener(v -> autoCompleteTextView.showDropDown());
        Button dlgbtn = dialog.findViewById(R.id.button5);
        dlgbtn.setOnClickListener(v -> {
            if (currentDistance != null && !selectedTags.isEmpty()) {
                args.putSerializable("selectedTags", selectedTags);
                dialog.dismiss();
                FragmentManager fragmentManager = requireActivity().getSupportFragmentManager();
                FragmentTransaction transaction = fragmentManager.beginTransaction();
                Fragment current = fragmentManager.findFragmentById(R.id.fragment_container);
                if (current != null) {
                    transaction.hide(current);
                }
                Fragment oldTarget = fragmentManager.findFragmentByTag(String.valueOf(R.id.newpathFragment));
                if (oldTarget != null) {
                    transaction.remove(oldTarget);
                }
                Fragment newTarget = NewRouteFragment.newInstance(selectedTags);
                transaction.add(R.id.fragment_container, newTarget, String.valueOf(R.id.newpathFragment));
                transaction.commit();
                BottomNavigationView bottomNavigationView = requireActivity().findViewById(R.id.bottomNavigationView);
                bottomNavigationView.setSelectedItemId(R.id.newpathFragment);
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).updateFragment(R.id.newpathFragment, newTarget);
                }
            } else {
                toastText.setText("Ничего не выбрано");
                toast.show();
            }
        });
        dialog.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}