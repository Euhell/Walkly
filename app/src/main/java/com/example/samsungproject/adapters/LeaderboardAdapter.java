package com.example.samsungproject.adapters;

import android.content.Context;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.samsungproject.R;
import com.example.samsungproject.types.LeaderboardUser;

import java.util.ArrayList;
import java.util.List;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.ViewHolder> {

    private final List<LeaderboardUser> userList = new ArrayList<>();
    private final Context context;

    public LeaderboardAdapter(Context context) {
        this.context = context;
    }

    public void setItems(List<LeaderboardUser> users) {
        userList.clear();
        userList.addAll(users);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public LeaderboardAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_leaderboard, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull LeaderboardAdapter.ViewHolder holder, int position) {
        LeaderboardUser user = userList.get(position);
        holder.nameTextView.setText(user.displayName != null ? user.displayName : "Без имени");
        String SavedUnits = PreferenceManager.getDefaultSharedPreferences(context)
                .getString("unit_pref", "Метры");
        switch (SavedUnits) {
            case "Мили":
                holder.scoreTextView.setText("Всего пройдено: " +
                        String.format("%.2f миль", (double) user.score / 1609.34));
                break;
            case "Футы":
                holder.scoreTextView.setText("Всего пройдено: " +
                        String.format("%.2f футов", (double) user.score / 0.3048));
                break;
            case "Ярды":
                holder.scoreTextView.setText("Всего пройдено: " +
                        String.format("%.2f ярдов", (double) user.score / 0.9144));
                break;
            case "Метры":
                if (user.score >= 1000) {
                    holder.scoreTextView.setText("Всего пройдено: " +
                            String.format("%.2f км", (double) user.score / 1000));
                } else {
                    holder.scoreTextView.setText("Всего пройдено: " +
                            String.format("%.0f м", (double) user.score));
                }
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + SavedUnits);
        }
        if (user.photoUrl != null && !user.photoUrl.isEmpty()) {
            Glide.with(context).load(user.photoUrl).into(holder.avatarImageView);
        } else {
            holder.avatarImageView.setImageResource(R.drawable.baseline_person_24);
        }
    }

    @Override
    public int getItemCount() {
        return userList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView avatarImageView;
        TextView nameTextView;
        TextView scoreTextView;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImageView = itemView.findViewById(R.id.avatarImageView);
            nameTextView = itemView.findViewById(R.id.nameTextView);
            scoreTextView = itemView.findViewById(R.id.scoreTextView);
        }
    }
}