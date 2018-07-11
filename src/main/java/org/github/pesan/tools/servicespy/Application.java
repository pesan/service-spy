package org.github.pesan.tools.servicespy;

import org.github.pesan.tools.servicespy.action.LocalDateTimeSerializer;
import org.github.pesan.tools.servicespy.action.RequestIdGenerator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;

@SpringBootApplication
public class Application {
    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    public RequestIdGenerator requestIdGenerator() {
        return () -> UUID.randomUUID().toString();
    }

    @Bean
    public Jackson2ObjectMapperBuilder builder() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        builder.serializerByType(LocalDateTime.class, new LocalDateTimeSerializer());
        return builder;
    }
}
