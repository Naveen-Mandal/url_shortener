package com.naveenmandal.urlshortener.service;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service  // marks this as a Spring-managed service bean
public class RedisService {

    // Key names — keep them as constants so no typos across the app
    private static final String URL_COUNTER_KEY = "url:counter";         // global INCR counter
    private static final String URL_CACHE_PREFIX = "url:";               // url:aB3x9 → longUrl
    private static final long DEFAULT_TTL_SECONDS = 86400L;              // 24 hours

    private final RedisTemplate<String, String> redisTemplate;

    // Constructor injection (preferred over @Autowired field injection)
    public RedisService(RedisTemplate<String, String> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    // ─── ID Generation ───────────────────────────────────────────────────

    /**
     * Atomically increments a global counter and returns the new value.
     *
     * Redis command: INCR url:counter
     * This is safe across multiple servers — Redis handles the atomicity.
     */
    public long generateNextId() {
        Long id = redisTemplate.opsForValue().increment(URL_COUNTER_KEY);
        return id != null ? id : 1L;
    }

    // ─── URL Cache (Cache-Aside Pattern) ─────────────────────────────────

    /**
     * Saves shortCode → longUrl in Redis with a TTL.
     *
     * Redis command: SET url:aB3x9 "https://google.com" EX 86400
     */
    public void cacheUrl(String shortCode, String longUrl) {
        String key = URL_CACHE_PREFIX + shortCode;
        redisTemplate.opsForValue().set(key, longUrl, DEFAULT_TTL_SECONDS, TimeUnit.SECONDS);
    }

    /**
     * Looks up a shortCode in Redis cache.
     *
     * Redis command: GET url:aB3x9
     * Returns null if key doesn't exist (cache miss → go to DB)
     */
    public String getCachedUrl(String shortCode) {
        return redisTemplate.opsForValue().get(URL_CACHE_PREFIX + shortCode);
    }

    /**
     * Removes a URL from cache (useful when a URL is deleted).
     *
     * Redis command: DEL url:aB3x9
     */
    public void evictUrl(String shortCode) {
        redisTemplate.delete(URL_CACHE_PREFIX + shortCode);
    }

    // ─── Click Analytics (Write-Behind) ──────────────────────────────────
    private static final String CLICK_COUNT_PREFIX = "clicks:";
    private static final String ACTIVE_CLICKS_SET = "clicks:active_keys";

    /**
     * Atomically increments the click counter in Redis.
     * Also records the shortCode in an active set so the scheduled sync
     * knows which URLs have pending clicks to flush.
     *
     * Redis commands:
     *   INCR clicks:aB3x9
     *   SADD clicks:active_keys "aB3x9"
     */
    public void recordClick(String shortCode) {
        String key = CLICK_COUNT_PREFIX + shortCode;
        redisTemplate.opsForValue().increment(key);
        // Track which short codes have unsynced clicks
        redisTemplate.opsForSet().add(ACTIVE_CLICKS_SET, shortCode);
    }

    /**
     * Gets pending clicks currently in Redis memory (not yet flushed to DB).
     */
    public long getPendingClicks(String shortCode) {
        String countStr = redisTemplate.opsForValue().get(CLICK_COUNT_PREFIX + shortCode);
        return countStr != null ? Long.parseLong(countStr) : 0L;
    }

    /**
     * Returns all short codes that have accumulated clicks.
     */
    public java.util.Set<String> getActiveClickCodes() {
        return redisTemplate.opsForSet().members(ACTIVE_CLICKS_SET);
    }

    /**
     * Atomically gets the current count and resets the key.
     * Using GETDEL (Get & Delete) ensures no clicks are lost during the sync!
     */
    public long getAndResetClicks(String shortCode) {
        String countStr = redisTemplate.opsForValue().getAndDelete(CLICK_COUNT_PREFIX + shortCode);
        redisTemplate.opsForSet().remove(ACTIVE_CLICKS_SET, shortCode);
        return countStr != null ? Long.parseLong(countStr) : 0L;
    }
}