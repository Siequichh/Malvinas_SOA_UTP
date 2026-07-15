package com.malvinas.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "jwt.secret=test-secret-key-for-gateway"
})
class ApiGatewayApplicationTests {
    @Test void contextLoads() {}
}
