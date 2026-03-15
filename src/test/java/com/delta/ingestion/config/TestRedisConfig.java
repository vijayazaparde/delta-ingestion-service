package com.delta.ingestion.config;

import io.github.bucket4j.Bucket;
import org.mockito.Mockito;
import org.redisson.api.RedissonClient;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import static org.mockito.Mockito.mock;

@TestConfiguration
public class TestRedisConfig {

    @Bean
    @Primary
    public RedissonClient redissonClient() {
        return mock(RedissonClient.class);
    }

    @Bean
    @Primary
    public Bucket ingestionBucket() {
        // Create a mock bucket that always allows requests
        Bucket mockBucket = mock(Bucket.class);
        Mockito.when(mockBucket.tryConsume(1)).thenReturn(true);
        return mockBucket;
    }
}