package com.naveenmandal.urlshortener.service;

import com.naveenmandal.urlshortener.dto.ShortenRequest;
import com.naveenmandal.urlshortener.dto.ShortenResponse;
import com.naveenmandal.urlshortener.model.UrlMapping;
import com.naveenmandal.urlshortener.repository.UrlMappingRepository;
import com.naveenmandal.urlshortener.util.Base62Util;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UrlShortenerService {

    // Read from application.yaml: app.shortener.base-url
    // If not found, defaults to "http://localhost:8080/"
    @Value("${app.shortener.base-url:http://localhost:8080/}")
    private String baseUrl;

    private final UrlMappingRepository repository;
    private final RedisService redisService;

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
    @Transactional
    public ShortenResponse shorten(ShortenRequest request) {
        String longUrl = request.getLongUrl();

        // Step 1 & 2: Generate unique short code
        // Redis INCR is atomic — safe even with 100 concurrent requests
        long uniqueId = redisService.generateNextId();
        String shortCode = Base62Util.encode(uniqueId);

        // Step 3: Persist to MySQL (source of truth)
        UrlMapping mapping = new UrlMapping();
        mapping.setShortCode(shortCode);
        mapping.setLongUrl(longUrl);
        repository.save(mapping);

        // Step 4: Cache in Redis
        // Next time someone hits this short URL → Redis serves it without DB query
        redisService.cacheUrl(shortCode, longUrl);

        // Step 5: Build and return the full short URL
        String shortUrl = baseUrl + shortCode;
        return new ShortenResponse(shortUrl, shortCode, longUrl);
    }
}