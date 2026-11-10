package com.naveenmandal.urlshortener.listener;


import com.naveenmandal.urlshortener.dto.ClickEvent;
import com.naveenmandal.urlshortener.service.RedisService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

@Component
public class ClickListener implements MessageListener {

    private static final Logger log = LoggerFactory.getLogger(ClickListener.class);

    private final RedisService redisService;
    private final ObjectMapper objectMapper;

    public ClickListener(RedisService redisService, ObjectMapper objectMapper) {
        this.redisService = redisService;
        this.objectMapper = objectMapper;
    }

    /**
     * Executes in a BACKGROUND thread pool whenever a message arrives on the Redis channel.
     * The user has already received their 302 redirect by the time this runs!
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        try {
            String json = new String(message.getBody());
            ClickEvent event = objectMapper.readValue(json, ClickEvent.class);

            log.info("Received ClickEvent asynchronously for shortCode: {}", event.getShortCode());

            // 1. Total Clicks & Leaderboard (Phase 6)
            redisService.recordClick(event.getShortCode());

            // 2. Unique Visitors HyperLogLog (Phase 7)
            if (event.getClientIp() != null && !event.getClientIp().isBlank()) {
                redisService.recordVisitor(event.getShortCode(), event.getClientIp());
            }

        } catch (Exception e) {
            log.error("Error processing ClickEvent from Redis channel: {}", e.getMessage(), e);
        }
    }
}