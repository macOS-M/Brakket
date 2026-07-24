package com.coffeecommits.brakket.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Fail-fast de la integración con Twitch (EPIC-10).
 *
 * <p>Fail-open en producción es el default equivocado: sin este chequeo, una
 * variable olvidada deja arrancar la app "bien" y el error aparece en runtime
 * frente al usuario. El caso especial es test/CI (sin secretos), que apaga el
 * chequeo con {@code brakket.twitch.required=false} / TWITCH_REQUIRED=false.</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TwitchStartupValidator implements InitializingBean {
    private final TwitchProperties properties;

    @Override
    public void afterPropertiesSet() {
        if (!properties.isRequired()) {
            log.info("Chequeo de credenciales de Twitch desactivado (TWITCH_REQUIRED=false).");
            return;
        }
        List<String> faltantes = new ArrayList<>();
        if (vacia(properties.getClientId())) faltantes.add("TWITCH_CLIENT_ID");
        if (vacia(properties.getClientSecret())) faltantes.add("TWITCH_CLIENT_SECRET");
        if (!faltantes.isEmpty()) {
            throw new IllegalStateException(
                    "Faltan variables de entorno de Twitch: " + String.join(", ", faltantes)
                            + ". Configúrelas en el .env de la raíz (ver .env.example)"
                            + " o, SOLO en test/CI, desactive el chequeo con TWITCH_REQUIRED=false.");
        }
        if (vacia(properties.getChannel())) {
            // El canal tiene la BD como fuente alternativa (RF-34): se avisa
            // pero no se aborta, porque puede haberse configurado desde /twitch.
            log.warn("TWITCH_CHANNEL está vacío: /transmisiones dependerá de un canal configurado en BD (RF-34).");
        }
    }

    private boolean vacia(String valor) {
        return valor == null || valor.isBlank();
    }
}
