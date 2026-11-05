package com.naveenmandal.urlshortener.scheduler;

import com.naveenmandal.urlshortener.service.UrlShortenerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ClickCountSyncScheduler {

    private static final Logger log = LoggerFactory.getLogger(ClickCountSyncScheduler.class);
    private final UrlShortenerService shortenerService;

    public ClickCountSyncScheduler(UrlShortenerService shortenerService) {
        this.shortenerService = shortenerService;
    }

    /**
     * Runs every 30 seconds (30,000 milliseconds).
     * Flushes buffered click counts from Redis into MySQL.
     */
    @Scheduled(fixedRate = 30000)
    public void syncClicks() {
        log.info("Starting background sync: flushing clicks from Redis to MySQL...");

        try {
            shortenerService.flushClicksToDatabase();
            log.info("Click sync completed successfully.");
        } catch (Exception e) {
            // If MySQL is down, you will see the exact error without crashing the app!
            log.error("Failed to sync clicks to database: {}", e.getMessage(), e);
        }
    }
}