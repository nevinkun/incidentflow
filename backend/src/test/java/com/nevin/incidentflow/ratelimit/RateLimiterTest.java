package com.nevin.incidentflow.ratelimit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private RateLimiter rateLimiter;

    @BeforeEach
    void setUp() {
        rateLimiter = new RateLimiter(redisTemplate, 100, 60);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
    }

    @Test
    void allowsRequestUnderLimit() {
        when(valueOperations.increment(anyString())).thenReturn(50L);

        assertThat(rateLimiter.isAllowed("monitoring-service")).isTrue();
    }

    @Test
    void deniesRequestOverLimit() {
        when(valueOperations.increment(anyString())).thenReturn(101L);

        assertThat(rateLimiter.isAllowed("monitoring-service")).isFalse();
    }

    @Test
    void setsExpiryOnlyOnFirstRequestInWindow() {
        when(valueOperations.increment(anyString())).thenReturn(1L);

        rateLimiter.isAllowed("monitoring-service");

        verify(redisTemplate).expire(eq("incidentflow:rate-limit:monitoring-service"), any(Duration.class));
    }

    @Test
    void doesNotResetExpiryOnSubsequentRequests() {
        when(valueOperations.increment(anyString())).thenReturn(5L);

        rateLimiter.isAllowed("monitoring-service");

        verify(redisTemplate, never()).expire(anyString(), any(Duration.class));
    }

    @Test
    void failsOpenWhenRedisIsUnreachable() {
        when(valueOperations.increment(anyString())).thenThrow(new QueryTimeoutException("Redis down"));

        assertThat(rateLimiter.isAllowed("monitoring-service")).isTrue();
    }
}
