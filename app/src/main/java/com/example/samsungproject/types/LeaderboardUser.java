package com.example.samsungproject.types;

public class LeaderboardUser {
    public String displayName;
    public long score;
    public String photoUrl;

    public LeaderboardUser() {}

    public LeaderboardUser(String displayName, long score, String photoUrl) {
        this.displayName = displayName;
        this.score = score;
        this.photoUrl = photoUrl;
    }
}
