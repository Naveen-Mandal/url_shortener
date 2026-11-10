package com.naveenmandal.urlshortener.dto;

import java.time.Instant;

public class ClickEvent {

    private String shortCode;
    private String clientIp;
    private Instant timestamp;

    // Default constructor needed for Jackson JSON deserialization
    public ClickEvent() {
    }

    public ClickEvent(String shortCode, String clientIp, Instant timestamp) {
        this.shortCode = shortCode;
        this.clientIp = clientIp;
        this.timestamp = timestamp;
    }

    public String getShortCode() { return shortCode; }
    public void setShortCode(String shortCode) { this.shortCode = shortCode; }

    public String getClientIp() { return clientIp; }
    public void setClientIp(String clientIp) { this.clientIp = clientIp; }

    public Instant getTimestamp() { return timestamp; }
    public void setTimestamp(Instant timestamp) { this.timestamp = timestamp; }
}