package com.coffeecommits.brakket.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Configuración del JWT que emite Brakket tras el login con Google.
 * Propiedades: brakket.jwt.*  (ver application.yml / .env)
 *
 * <p>Fail-fast al arrancar, igual que {@link TwitchStartupValidator}: antes
 * había un secret por defecto commiteado en el repo, y con él cualquiera que
 * leyera el código podía firmar tokens de cualquier rol. A diferencia de
 * Twitch no hay interruptor para saltarse el chequeo — un secret de prueba
 * es trivial de proveer (los tests lo definen en application.properties).</p>
 */
@Component
@ConfigurationProperties(prefix = "brakket.jwt")
public class JwtProperties implements InitializingBean {

    /** HS256 exige una clave de al menos 256 bits. */
    private static final int LARGO_MINIMO = 32;

    /** Clave secreta para firmar los tokens. Sin valor por defecto a propósito. */
    private String secret;

    /** Vigencia del token en milisegundos (por defecto 24h). */
    private long expirationMs = 86_400_000L;

    @Override
    public void afterPropertiesSet() {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "Falta la variable de entorno JWT_SECRET. Configúrela en el .env"
                            + " de la raíz (ver .env.example) con al menos "
                            + LARGO_MINIMO + " caracteres.");
        }
        if (secret.length() < LARGO_MINIMO) {
            throw new IllegalStateException(
                    "JWT_SECRET tiene " + secret.length() + " caracteres; el mínimo para"
                            + " firmar con HS256 es " + LARGO_MINIMO + ".");
        }
    }

    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }

    public long getExpirationMs() { return expirationMs; }
    public void setExpirationMs(long expirationMs) { this.expirationMs = expirationMs; }
}
