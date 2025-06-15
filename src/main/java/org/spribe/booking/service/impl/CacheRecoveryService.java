package org.spribe.booking.service.impl;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.spribe.booking.repository.UnitRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class CacheRecoveryService {
    private final CacheManager cacheManager;
    private final UnitRepository unitRepository;
    private final RedisConnectionFactory redisConnectionFactory;

    @PostConstruct
    public void initializeCacheOnStartup() {
        recoverCache();
    }

    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void recoverCache() {
        log.info("Starting cache recovery process");

        try {
            // Check if Redis is available
            if (!isRedisAvailable()) {
                log.warn("Redis is not available, skipping cache recovery");
                return;
            }

            // Get the actual count from the database
            Long actualCount = unitRepository.countAvailableUnits();
            log.info("Actual available units count from database: {}", actualCount);

            // Get the current cached value
            Object cachedValue = cacheManager.getCache("availableUnits").get("count", Object.class);
            Long cachedCount = null;
            if (cachedValue instanceof Integer) {
                cachedCount = ((Integer) cachedValue).longValue();
            } else if (cachedValue instanceof Long) {
                cachedCount = (Long) cachedValue;
            }

            log.info("Current cached count: {}", cachedCount);

            // Update the cache with the actual count
            cacheManager.getCache("availableUnits").put("count", actualCount);
            log.info("Cache recovered successfully with count: {}", actualCount);
        } catch (Exception e) {
            log.error("Error during cache recovery: {}", e.getMessage(), e);
        }
    }

    private boolean isRedisAvailable() {
        try {
            redisConnectionFactory.getConnection().ping();
            return true;
        } catch (Exception e) {
            log.error("Redis connection error: {}", e.getMessage());
            return false;
        }
    }
}