package com.cmbchina.datadirect.caliber.infrastructure.common.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    private final CaliberCacheProperties caliberCacheProperties;

    public CacheConfig(CaliberCacheProperties caliberCacheProperties) {
        this.caliberCacheProperties = caliberCacheProperties;
    }

    @Bean
    public CacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        if (caliberCacheProperties.isRedisEnabled()) {
            RedisCacheConfiguration config = RedisCacheConfiguration.defaultCacheConfig()
                    .entryTtl(Duration.ofSeconds(Math.max(30, caliberCacheProperties.getTtlSeconds())))
                    .disableCachingNullValues();
            return RedisCacheManager.builder(redisConnectionFactory)
                    .cacheDefaults(config)
                    .build();
        }
        return new ConcurrentMapCacheManager("sceneById", "sceneList");
    }
}
