package com.malvinas.personal.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Malvinas SOA - Personal Service API")
                        .description("Gestion de empleados, roles y asistencia del personal")
                        .version("1.0.0"));
    }
}
