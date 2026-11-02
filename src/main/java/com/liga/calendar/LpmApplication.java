package com.liga.calendar;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.time.Clock;
import java.time.ZoneId;

@SpringBootApplication
public class LpmApplication {
    public static void main(String[] args) {
        SpringApplication.run(LpmApplication.class, args);
    }

    // A bean (rather than Clock.systemDefaultZone() called directly where
    // needed) so time-sensitive logic like the deck reservation cutoff can
    // be tested against a fixed instant instead of the real clock.
    @Bean
    public Clock clock() {
        return Clock.system(ZoneId.of("America/Montevideo"));
    }
}
