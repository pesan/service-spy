package org.github.pesan.tools.servicespy;

import io.vertx.core.Vertx;

import javax.annotation.PostConstruct;

import org.github.pesan.tools.servicespy.proxy.ProxyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Application {
    public static void main(String... args) {
        SpringApplication.run(Application.class, args);
    }

    public @Autowired ProxyService proxyService;

    @PostConstruct
    public void deploy() {
        Vertx.vertx().deployVerticle(proxyService);
    }

}
