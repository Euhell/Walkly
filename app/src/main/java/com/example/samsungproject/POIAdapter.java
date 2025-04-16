package com.example.samsungproject;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

public class POIAdapter extends RecyclerView.Adapter<POIAdapter.ViewHolder> {

    private List<POI> poiList;
    private List<POI> selectedTags;

    public POIAdapter(List<POI> poiList, List<POI> selectedTags) {
        this.poiList = poiList;
        this.selectedTags = selectedTags;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        CheckBox checkBox;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            checkBox = itemView.findViewById(R.id.checkbox);
        }
    }

    @NonNull
    @Override
    public POIAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_poi_checkbox, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull POIAdapter.ViewHolder holder, int position) {
        POI poi = poiList.get(position);
        holder.checkBox.setText(poi.label);
        holder.checkBox.setChecked(selectedTags.contains(poi));
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                if (!selectedTags.contains(poi)) selectedTags.add(poi);
            } else {
                selectedTags.remove(poi);
            }
        });
    }

    @Override
    public int getItemCount() {
        return poiList.size();
    }

    public List<POI> getSelectedPOIs() {
        List<POI> selected = new ArrayList<>();
        for (POI poi : poiList) {
            if (poi.isSelected) {
                selected.add(poi);
            }
        }
        return selected;
    }
}
