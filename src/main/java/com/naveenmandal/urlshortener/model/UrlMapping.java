package com.naveenmandal.urlshortener.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "url_mapping")
public class UrlMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "long_url", nullable = false, length = 2048)
    private String longUrl;

    @Column(name = "short_code", nullable = false, length = 10, unique = true)
    private String shortCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "click_count")
    private Long clickCount = 0L;

    @PrePersist
    public void prePersist(){
        this.createdAt = LocalDateTime.now();
    }
    public Long getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }
    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }
    public String getLongUrl() {
        return longUrl;
    }
    public void setLongUrl(String longUrl) {
        this.longUrl = longUrl;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public Long getClickCount() {
        return clickCount;
    }
    public void setClickCount(Long clickCount) {
        this.clickCount = clickCount;
    }
}
