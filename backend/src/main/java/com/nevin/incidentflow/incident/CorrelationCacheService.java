package com.nevin.incidentflow.incident;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;

@Component
public class CorrelationCacheService {

    private static final Logger log = LoggerFactory.getLogger(CorrelationCacheService.class);
    private static final String KEY_PREFIX = "incidentflow:correlation:";

    private final StringRedisTemplate redisTemplate;
    private final Duration ttl;

    public CorrelationCacheService(
            StringRedisTemplate redisTemplate,
            @Value("${incidentflow.correlation.window-minutes:15}") long windowMinutes) {
        this.redisTemplate = redisTemplate;
        this.ttl = Duration.ofMinutes(windowMinutes);
    }

    public Optional<UUID> get(String fingerprint) {
        try {
            String value = redisTemplate.opsForValue().get(KEY_PREFIX + fingerprint);
            return value == null ? Optional.empty() : Optional.of(UUID.fromString(value));
        } catch (DataAccessException e) {
            log.warn("Correlation cache read failed for fingerprint={}, falling back to database", fingerprint, e);
            return Optional.empty();
        }
    }

    public void put(String fingerprint, UUID incidentId) {
        try {
            redisTemplate.opsForValue().set(KEY_PREFIX + fingerprint, incidentId.toString(), ttl);
        } catch (DataAccessException e) {
            log.warn("Correlation cache write failed for fingerprint={}", fingerprint, e);
        }
    }

    public void evict(String fingerprint) {
        try {
            redisTemplate.delete(KEY_PREFIX + fingerprint);
        } catch (DataAccessException e) {
            log.warn("Correlation cache evict failed for fingerprint={}", fingerprint, e);
        }
    }
}
