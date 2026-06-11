package com.malvinas.cargas;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
@EnableJpaAuditing
public class CargasServiceApplication {

    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
    }

    public static void main(String[] args) {
        SpringApplication.run(CargasServiceApplication.class, args);
    }
}
