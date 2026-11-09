package com.naveenmandal.urlshortener.dto;

public class LeaderboardEntry {

    private String shortCode;
    private long clicks;

    public LeaderboardEntry(String shortCode, long clicks) {
        this.shortCode = shortCode;
        this.clicks = clicks;
    }

    public String getShortCode() { return shortCode; }
    public long getClicks() { return clicks; }
}