package com.naveenmandal.urlshortener.dto;

public class ShortenResponse {
    private String shortCode;
    private String shortUrl;
    private String longUrl;

    public ShortenResponse( String shortUrl, String shortCode, String longUrl) {
        this.shortCode = shortCode;
        this.shortUrl = shortUrl;
        this.longUrl = longUrl;
    }
    public String getShortCode() {
        return shortCode;
    }
    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
    public String getShortUrl() {
        return shortUrl;
    }
    public void setShortUrl(String shortUrl) {
        this.shortUrl = shortUrl;
    }
    public String getLongUrl() {
        return longUrl;
    }
    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }

}
