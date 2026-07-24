package com.coffeecommits.brakket.upload;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.util.concurrent.TimeUnit;

/**
 * Sirve las imágenes subidas como recurso estático: GET /api/uploads/xyz.png
 * lee del directorio de uploads. El nombre es un UUID inmutable, así que el
 * navegador puede cachear agresivamente.
 */
@Configuration
public class UploadsStaticConfig implements WebMvcConfigurer {

    private final String directorio;

    public UploadsStaticConfig(@Value("${brakket.uploads.dir:uploads}") String dir) {
        this.directorio = Path.of(dir).toAbsolutePath().normalize().toUri().toString();
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/api/uploads/**")
                .addResourceLocations(directorio)
                .setCachePeriod((int) TimeUnit.DAYS.toSeconds(30));
    }
}
