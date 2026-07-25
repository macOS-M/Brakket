package com.coffeecommits.brakket.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Fallback para rutas de autenticación que pertenecen a la SPA.
 *
 * <p>Si el navegador termina en el backend por error, redirigimos al frontend
 * para evitar que Spring Boot muestre la Whitelabel 404.</p>
 */
@RestController
@RequestMapping("/auth")
public class FrontendRedirectController {

    @Value("${brakket.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @GetMapping("/login")
    public ResponseEntity<Void> redirectLogin() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.LOCATION, frontendUrl + "/auth/login");
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }

    @GetMapping("/callback")
    public ResponseEntity<Void> redirectCallback(@RequestParam(value = "token", required = false) String token) {
        HttpHeaders headers = new HttpHeaders();
        String target = frontendUrl + "/auth/callback" + (token == null ? "" : "?token=" + token);
        headers.set(HttpHeaders.LOCATION, target);
        return new ResponseEntity<>(headers, HttpStatus.FOUND);
    }
}