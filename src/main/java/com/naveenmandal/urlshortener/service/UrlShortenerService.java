package com.naveenmandal.urlshortener.service;

import com.naveenmandal.urlshortener.dto.ClickEvent;
import com.naveenmandal.urlshortener.dto.LeaderboardEntry;
import com.naveenmandal.urlshortener.dto.ShortenRequest;
import com.naveenmandal.urlshortener.dto.ShortenResponse;
import com.naveenmandal.urlshortener.model.UrlMapping;
import com.naveenmandal.urlshortener.repository.UrlMappingRepository;
import com.naveenmandal.urlshortener.util.Base62Util;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class UrlShortenerService {

    // Read from application.yaml: app.shortener.base-url
    // If not found, defaults to "http://localhost:8080/"
    @Value("${app.shortener.base-url:http://localhost:8080/}")
    private String baseUrl;

    private final UrlMappingRepository repository;
    private final RedisService redisService;

    private static final Logger log = LoggerFactory.getLogger(UrlShortenerService.class);

    // Constructor injection — always prefer this over @Autowired on fields
    // Reason: makes dependencies explicit, easier to unit test
    public UrlShortenerService(UrlMappingRepository repository, RedisService redisService) {
        this.repository = repository;
        this.redisService = redisService;
    }

    /**
     * Core business logic for shortening a URL.
     *
     * Flow:
     *  1. INCR url:counter in Redis → get unique number
     *  2. Base62 encode the number → get short code (e.g. "aB3x9")
     *  3. Save to MySQL (permanent)
     *  4. Cache in Redis with TTL (fast future lookups)
     *  5. Return the short URL
     *
     * @Transactional ensures: if DB save fails, everything rolls back.
     * It does NOT protect Redis (Redis has no transactions with DB),
     * but that's acceptable — Redis is a cache, DB is source of truth.
     */
//    @Transactional(noRollbackFor = DataIntegrityViolationException.class)
    public ShortenResponse shorten(ShortenRequest request) {
        String longUrl = request.getLongUrl();

        // ── Step 0: Compute SHA-256 hash of the longUrl ───────────────────
        // Why hash? Because longUrl can be 2048 chars — too long for a DB index.
        // SHA-256 always produces 64 hex chars. Safe to index.
        // DigestUtils comes from Apache Commons Codec (already in Spring Boot via spring-boot-starter-web)
        String hash = DigestUtils.sha256Hex(longUrl);

        // ── Step 1: Check if this URL was already shortened ───────────────
        // We check by HASH, not by the full longUrl string
        Optional<UrlMapping> existing = repository.findByLongUrlHash(hash);
        if (existing.isPresent()) {
            // Already exists → return same short URL (idempotent) ✅
            String shortCode = existing.get().getShortCode();
            return new ShortenResponse(baseUrl + shortCode, shortCode, longUrl);
        }

        // ── Step 2: Generate a new short code ────────────────────────────
        long uniqueId = redisService.generateNextId();
        String shortCode = Base62Util.encode(uniqueId);

        // ── Step 3: Build the entity ──────────────────────────────────────
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setLongUrl(longUrl);
        mapping.setLongUrlHash(hash);   // ← store the hash too

        // ── Step 4: Try saving to MySQL ───────────────────────────────────
        try {
            repository.save(mapping);
        } catch (DataIntegrityViolationException e) {
            // RACE CONDITION CAUGHT ✅
            // Two threads both saw "not found", both tried to save.
            // One won (saved first), one lost (gets this exception).
            // The loser: look up the winner's record and return it.
            //
            // Why not just let it throw? Because the user still needs a valid response.
            // The URL WAS shortened — just by the other thread.
            UrlMapping saved = repository.findByLongUrlHash(hash)
                    .orElseThrow(() -> new RuntimeException("Unexpected: hash not found after conflict"));
            String existingCode = saved.getShortCode();
            return new ShortenResponse(baseUrl + existingCode, existingCode, longUrl);
        }

        // ── Step 5: Cache in Redis & return ──────────────────────────────
        redisService.cacheUrl(shortCode, longUrl);
        return new ShortenResponse(baseUrl + shortCode, shortCode, longUrl);
    }

    public Optional<String> resolve(String shortCode, String clientIp){
        String cachedLongUrl = redisService.getCachedUrl(shortCode);
        if(cachedLongUrl != null){
            // Record click in Redis (FAST PATH)
//            trackClickSilently(shortCode, clientIp);
            publishClickEvent(shortCode, clientIp);  //asynchronous analytics
            return Optional.of(cachedLongUrl);
        }
        Optional<UrlMapping> dbLongUrl = repository.findByShortCode(shortCode);
        if(dbLongUrl.isPresent()){
            redisService.cacheUrl( shortCode, dbLongUrl.get().getLongUrl());
            // Record click in Redis
//            trackClickSilently(shortCode, clientIp);
            publishClickEvent(shortCode, clientIp);  //asynchronous analytics.
            return Optional.of(dbLongUrl.get().getLongUrl());
        }

        return Optional.empty();

    }


    /**
     * Pub/Sub decoupled analytics:
     * Only emits a lightweight event to Redis and returns immediately!
     * ClickListener will process Counter, Leaderboard, and HLL in the background.
     */
    private void publishClickEvent(String shortCode, String clientIp) {
        ClickEvent event = new ClickEvent(shortCode, clientIp, Instant.now());
        redisService.publishClickEvent(event);
    }

//    /**
//     * Prevents analytics exceptions from breaking user redirect
//     */
//    private void trackClickSilently(String shortCode, String clientIp) {
//        try {
//            // 1. Total clicks & Leaderboard
//            redisService.recordClick(shortCode);
//            // 2. Unique visitor count -HyperLogLog.
//            if (clientIp != null && !clientIp.isBlank()) {
//                redisService.recordVisitor(shortCode, clientIp);
//            }
//        } catch (Exception e) {
//            log.warn("Failed to record click analytics for shortCode {}: {}", shortCode, e.getMessage());
//        }
//    }


    /**
     * Returns unique visitor count from HyperLogLog
     */
    public long getUniqueVisitors(String shortCode) {
        return redisService.getUniqueVisitorCount(shortCode);
    }



    /**
     * Converts Redis ZSet tuples to LeaderboardEntry DTOs
     */
    public List<LeaderboardEntry> getTopLeaderboard(int limit) {
        Set<ZSetOperations.TypedTuple<String>> rawEntries = redisService.getTopUrls(limit);
        List<LeaderboardEntry> result = new ArrayList<>();
        for (ZSetOperations.TypedTuple<String> tuple : rawEntries) {
            if (tuple.getValue() != null) {
                long score = tuple.getScore() != null ? tuple.getScore().longValue() : 0L;
                result.add(new LeaderboardEntry(tuple.getValue(), score));
            }
        }
        return result;
    }


    /**
     * Real-time click count = DB count + pending Redis buffer count.
     */
    public Optional<Long> getTotalClicks(String shortCode) {
        Optional<UrlMapping> mapping = repository.findByShortCode(shortCode);
        if (mapping.isEmpty()) {
            return Optional.empty();
        }

        long dbClicks = mapping.get().getClickCount();
        long redisPendingClicks = redisService.getPendingClicks(shortCode);

        return Optional.of(dbClicks + redisPendingClicks);
    }


    /**
     * Flushes accumulated clicks from Redis to MySQL.
     */
    public void flushClicksToDatabase() {
        Set<String> activeCodes = redisService.getActiveClickCodes();
        if (activeCodes == null || activeCodes.isEmpty()) {
            return;
        }

        for (String shortCode : activeCodes) {
            long pendingClicks = redisService.getAndResetClicks(shortCode);
            if (pendingClicks > 0) {
                repository.incrementClickCount(shortCode, pendingClicks);
            }
        }
    }
}