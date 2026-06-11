package com.malvinas.reportes;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import java.util.TimeZone;

@SpringBootApplication
@EnableDiscoveryClient
public class ReportesServiceApplication {

    @PostConstruct
    void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("America/Bogota"));
    }

    public static void main(String[] args) {
        SpringApplication.run(ReportesServiceApplication.class, args);
    }
}
