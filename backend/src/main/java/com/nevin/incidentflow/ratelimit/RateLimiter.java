package com.nevin.incidentflow.ratelimit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class RateLimiter {

    private static final Logger log = LoggerFactory.getLogger(RateLimiter.class);

    private final StringRedisTemplate redisTemplate;
    private final int maxRequests;
    private final Duration window;

    public RateLimiter(
            StringRedisTemplate redisTemplate,
            @Value("${incidentflow.rate-limit.max-requests:100}") int maxRequests,
            @Value("${incidentflow.rate-limit.window-seconds:60}") long windowSeconds) {
        this.redisTemplate = redisTemplate;
        this.maxRequests = maxRequests;
        this.window = Duration.ofSeconds(windowSeconds);
    }

    public boolean isAllowed(String source) {
        String key = "incidentflow:rate-limit:" + source;

        try {
            Long count = redisTemplate.opsForValue().increment(key);

            if (count != null && count == 1L) {
                redisTemplate.expire(key, window);
            }

            return count != null && count <= maxRequests;
        } catch (DataAccessException e) {
            log.warn("Rate limiter Redis call failed for source={}, allowing request through", source, e);
            return true;
        }
    }
}
