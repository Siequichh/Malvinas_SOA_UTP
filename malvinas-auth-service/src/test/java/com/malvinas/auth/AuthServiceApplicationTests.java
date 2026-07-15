package com.malvinas.auth;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
    "eureka.client.enabled=false",
    "spring.cloud.discovery.enabled=false",
    "jwt.secret=test-secret-key-for-auth-service"
})
class AuthServiceApplicationTests {
    @Test void contextLoads() {}
}
