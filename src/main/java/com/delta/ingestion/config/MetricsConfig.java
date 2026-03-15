package com.delta.ingestion.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MetricsConfig {

    @Bean
    public Counter ingestionRecordsCounter(MeterRegistry registry) {
        return registry.counter("ingestion_records_total");
    }

    @Bean
    public Counter ingestionFailureCounter(MeterRegistry registry) {
        return registry.counter("ingestion_failures_total");
    }

    @Bean
    public Counter recordsInsertedCounter(MeterRegistry registry) {
        return registry.counter("ingestion_records_inserted");
    }

    @Bean
    public Counter recordsSkippedCounter(MeterRegistry registry) {
        return registry.counter("ingestion_records_skipped");
    }

    @Bean
    public Counter recordsFailedCounter(MeterRegistry registry) {
        return registry.counter("ingestion_records_failed");
    }

    @Bean
    public Timer copyThroughputTimer(MeterRegistry registry) {
        return Timer.builder("ingestion_copy_duration")
                .description("Time taken for PG COPY operations")
                .register(registry);
    }
}