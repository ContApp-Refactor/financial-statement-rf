package com.unicauca.edu.co.financial_statements.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfiguration {

    @Bean
    public Clock systemClock() {
        return Clock.systemDefaultZone();
    }
}
