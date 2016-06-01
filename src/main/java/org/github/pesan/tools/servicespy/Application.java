package org.github.pesan.tools.servicespy;

import io.vertx.core.Vertx;
import org.github.pesan.tools.servicespy.action.RequestIdGenerator;
import org.github.pesan.tools.servicespy.proxy.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.annotation.PostConstruct;
import java.time.Clock;
import java.util.UUID;

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

    public @Autowired ProxyService proxyService;

    @PostConstruct
    public void deploy() {
        Vertx.vertx().deployVerticle(proxyService);
    }

}
