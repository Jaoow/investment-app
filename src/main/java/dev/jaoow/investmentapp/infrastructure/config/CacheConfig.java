package dev.jaoow.investmentapp.infrastructure.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.annotation.Value;

import java.time.Duration;

@Configuration
@EnableCaching
public class CacheConfig {

    @Bean
    public Caffeine<Object, Object> caffeineConfig(
            @Value("${brapi.quote.cache-ttl-seconds:60}") long cacheTtlSeconds,
            @Value("${brapi.quote.cache-max-size:1000}") long cacheMaxSize) {
        if (cacheTtlSeconds <= 0 || cacheMaxSize <= 0) {
            throw new IllegalArgumentException("Quote cache TTL and maximum size must be positive.");
        }
        return Caffeine.newBuilder()
                .maximumSize(cacheMaxSize)
                .expireAfterWrite(Duration.ofSeconds(cacheTtlSeconds));
    }

    @Bean
    public CacheManager cacheManagerWithRefresh(Caffeine<Object, Object> caffeine) {
        CaffeineCacheManager caffeineCacheManager = new CaffeineCacheManager();
        caffeineCacheManager.setCaffeine(caffeine);
        return caffeineCacheManager;
    }
}
