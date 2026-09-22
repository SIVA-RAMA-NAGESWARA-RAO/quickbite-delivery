package com.quickbite.order.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

/**
 * A single injectable Clock bean, used by SurgePricingService. Routing
 * "the current time" through a bean (instead of calling LocalTime.now()
 * directly) means tests can pin a fixed instant instead of behaving
 * differently depending on what time of day they happen to run.
 */
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }
}
