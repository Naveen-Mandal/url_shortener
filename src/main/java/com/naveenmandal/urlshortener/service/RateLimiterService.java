package com.naveenmandal.urlshortener.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
public class RateLimiterService {

    private final StringRedisTemplate redisTemplate;
    private final DefaultRedisScript<Long> rateLimitScript;

    public RateLimiterService(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;

        // Atomic Lua Script: INCR and set EXPIRE only on the 1st request
        String luaScript =
                "local current = redis.call('INCR', KEYS[1])\n" +
                        "if current == 1 then\n" +
                        "    redis.call('EXPIRE', KEYS[1], ARGV[1])\n" +
                        "end\n" +
                        "return current";

        this.rateLimitScript = new DefaultRedisScript<>();
        this.rateLimitScript.setScriptText(luaScript);
        this.rateLimitScript.setResultType(Long.class);
    }

    /**
     * Checks if the given key is allowed under the rate limit.
     *
     * @param key           Redis key (e.g. "rate:shorten:192.168.1.1")
     * @param maxRequests   Maximum requests allowed in the window
     * @param windowSeconds Window duration in seconds
     * @return true if request is allowed, false if limit exceeded
     */
    public boolean isAllowed(String key, long maxRequests, long windowSeconds) {
        Long currentCount = redisTemplate.execute(
                rateLimitScript,
                Collections.singletonList(key),
                String.valueOf(windowSeconds)
        );

        return currentCount != null && currentCount <= maxRequests;
    }

    /**
     * Returns remaining TTL (in seconds) so we can populate the Retry-After header.
     */
    public long getRemainingTtl(String key) {
        Long ttl = redisTemplate.getExpire(key);
        return (ttl != null && ttl > 0) ? ttl : 60L;
    }
}