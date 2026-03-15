package com.delta.ingestion.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import io.github.bucket4j.grid.jcache.JCacheProxyManager;
import org.redisson.api.RedissonClient;
import org.redisson.jcache.configuration.RedissonConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import javax.cache.Cache;
import javax.cache.CacheManager;
import javax.cache.Caching;
import java.time.Duration;

@Configuration
@Profile("!test")
public class RateLimitConfig {

    @Bean
    public Bucket ingestionBucket(RedissonClient redissonClient) {
        // 1. Define the Global Limit
        Bandwidth limit = Bandwidth.classic(5, Refill.greedy(5, Duration.ofMinutes(1)));

        // 2. Initialize JCache Provider using Redisson
        CacheManager cacheManager = Caching.getCachingProvider().getCacheManager();

        Cache<String, byte[]> cache = cacheManager.getCache("ingestion-limit-cache");
        if (cache == null) {
            cache = cacheManager.createCache("ingestion-limit-cache",
                    RedissonConfiguration.fromInstance(redissonClient));
        }

        // 3. Create a ProxyManager
        ProxyManager<String> proxyManager = new JCacheProxyManager<>(cache);

        // 4. FIX: Return a Proxy Bucket using BucketConfiguration
        return proxyManager.builder().build("global-ingest-limit", () ->
                BucketConfiguration.builder()
                        .addLimit(limit)
                        .build()
        );
    }
}