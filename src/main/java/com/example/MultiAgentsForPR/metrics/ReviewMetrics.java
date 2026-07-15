package com.example.MultiAgentsForPR.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class ReviewMetrics {

    private final MeterRegistry registry;

    public ReviewMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordReviewDuration(long millis, String verdict) {
        Timer.builder("pr_review_duration")
                .description("Time taken to complete a full PR review")
                .tag("verdict", verdict)
                .register(registry)
                .record(millis, TimeUnit.MILLISECONDS);
    }

    public void recordAgentDuration(String agentName, long millis) {
        Timer.builder("agent_execution_duration")
                .description("Time taken by an individual agent to complete")
                .tag("agent", agentName)
                .register(registry)
                .record(millis, TimeUnit.MILLISECONDS);
    }

    public void incrementAgentFailure(String agentName) {
        registry.counter("agent_failures_total", "agent", agentName).increment();
    }

    public void incrementRetryAttempt(String agentName) {
        registry.counter("agent_retry_attempts_total", "agent", agentName).increment();
    }

    public void incrementCircuitBreakerFallback(String agentName) {
        registry.counter("circuit_breaker_fallbacks_total", "agent", agentName).increment();
    }

    public void incrementIdempotentDuplicate() {
        registry.counter("webhook_duplicate_deliveries_total").increment();
    }

    public void incrementReviewsByVerdict(String verdict) {
        registry.counter("pr_reviews_total", "verdict", verdict).increment();
    }
}