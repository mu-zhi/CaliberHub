package com.caliberhub.infrastructure.common.cache;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 简单内存缓存实现（带 TTL）
 */
@Slf4j
@Component
public class SimpleCache {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);
    
    private final Map<String, CacheEntry> cache = new ConcurrentHashMap<>();
    
    public <T> void put(String key, T value) {
        put(key, value, DEFAULT_TTL);
    }
    
    public <T> void put(String key, T value, Duration ttl) {
        cache.put(key, new CacheEntry(value, Instant.now().plus(ttl)));
        log.debug("Cache put: {} (TTL: {})", key, ttl);
    }
    
    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key) {
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.isExpired()) {
            cache.remove(key);
            log.debug("Cache expired: {}", key);
            return Optional.empty();
        }
        return Optional.of((T) entry.value());
    }
    
    public void evict(String key) {
        cache.remove(key);
        log.debug("Cache evict: {}", key);
    }
    
    public void clear() {
        cache.clear();
        log.info("Cache cleared");
    }
    
    private record CacheEntry(Object value, Instant expiresAt) {
        boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }
    }
}
