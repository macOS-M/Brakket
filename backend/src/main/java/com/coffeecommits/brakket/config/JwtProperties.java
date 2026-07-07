package com.coffeecommits.brakket.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del JWT que emite Brakket tras el login con Google.
 * Propiedades: brakket.jwt.*  (ver application.yml / .env)
 */
@Component
@ConfigurationProperties(prefix = "brakket.jwt")
public class JwtProperties {

    /** Clave secreta para firmar los tokens (mínimo 32 caracteres). */
    private String secret = "cambiar-esta-clave-secreta-de-al-menos-32-chars";

    /** Vigencia del token en milisegundos (por defecto 24h). */
    private long expirationMs = 86_400_000L;

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
