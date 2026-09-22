package com.quickbite.order.config;

import feign.Retryer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * A short, bounded retry for transient network blips talking to
 * restaurant-service / payment-service. Resilience4j (see application.yml)
 * layers a circuit breaker on top of this for sustained outages.
 */
@Configuration
public class FeignRetryConfig {

    @Bean
    public Retryer feignRetryer() {
        // period, maxPeriod, maxAttempts
        return new Retryer.Default(100, 500, 3);
    }
}
