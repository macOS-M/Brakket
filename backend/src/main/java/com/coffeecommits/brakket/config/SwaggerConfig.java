package com.coffeecommits.brakket.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI/Swagger.
 * Disponible en /swagger-ui.html una vez levantada la app.
 */
@Configuration
public class SwaggerConfig {

    @Bean
    public OpenAPI brakketOpenAPI() {
        return new OpenAPI().info(new Info()
                .title("Brakket API")
                .description("API REST de Brakket - gestión y transmisión de ligas y torneos de esports")
                .version("v0.1.0"));
    }
}
