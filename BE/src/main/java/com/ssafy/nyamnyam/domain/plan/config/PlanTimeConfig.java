package com.ssafy.nyamnyam.domain.plan.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.time.Clock;

@Configuration
public class PlanTimeConfig {
    @Bean public Clock planClock() { return Clock.systemUTC(); }
}
