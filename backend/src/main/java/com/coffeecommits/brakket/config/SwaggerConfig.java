package com.coffeecommits.brakket.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadatos de la documentación OpenAPI/Swagger.
 * Disponible en /swagger-ui/index.html una vez levantada la app.
 */
@Configuration
public class SwaggerConfig {

    private static final String JWT_SCHEME = "bearerAuth";

    @Bean
    public OpenAPI brakketOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Brakket API")
                        .description("API REST de Brakket - gestión y transmisión de ligas y torneos de esports")
                        .version("v0.1.0"))
                .addSecurityItem(new SecurityRequirement().addList(JWT_SCHEME))
                .components(new Components()
                        .addSecuritySchemes(JWT_SCHEME, new SecurityScheme()
                                .name(JWT_SCHEME)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
