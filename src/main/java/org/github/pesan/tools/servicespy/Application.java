package org.github.pesan.tools.servicespy;

import java.time.Clock;
import java.util.UUID;

import org.github.pesan.tools.servicespy.action.RequestIdGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class Application {
    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public RequestIdGenerator requestIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }
}
