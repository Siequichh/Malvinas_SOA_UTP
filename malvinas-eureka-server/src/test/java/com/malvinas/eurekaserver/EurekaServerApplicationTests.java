package com.malvinas.eurekaserver;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.register-with-eureka=false",
    "eureka.client.fetch-registry=false",
    "server.port=0"
})
class EurekaServerApplicationTests {
    @Test void contextLoads() {}
}
