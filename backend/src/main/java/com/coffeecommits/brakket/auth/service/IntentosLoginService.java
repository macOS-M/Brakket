package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Freno de fuerza bruta del login local: 5 fallos seguidos por correo
 * bloquean nuevos intentos durante 15 minutos.
 *
 * <p>El contador vive en memoria a propósito: reiniciar el backend lo limpia,
 * no necesita tabla ni migración, y para un solo nodo alcanza. Se cuenta por
 * correo y no por IP porque detrás de un NAT (el laboratorio de la U) una IP
 * agrupa a medio salón. El costo de esa decisión es que alguien puede
 * bloquearle el login a un correo ajeno a propósito durante 15 minutos; entre
 * los dos males, es el reversible.</p>
 */
@Component
public class IntentosLoginService {

    static final int MAX_FALLOS = 5;
    static final Duration BLOQUEO = Duration.ofMinutes(15);

    private final Clock clock;
    private final Map<String, Registro> registros = new ConcurrentHashMap<>();

    public IntentosLoginService() {
        this(Clock.systemDefaultZone());
    }

    /** Visible para tests: permite avanzar el tiempo sin esperar 15 minutos. */
    IntentosLoginService(Clock clock) {
        this.clock = clock;
    }

    /**
     * Va ANTES de comparar la contraseña: si el correo está bloqueado no se
     * gasta BCrypt (que es caro a propósito) ni se le da al atacante el
     * "incorrectos" que confirma que el intento se evaluó.
     */
    public void verificarBloqueo(String correo) {
        Registro registro = registros.get(correo);
        if (registro == null) {
            return;
        }
        Instant ahora = clock.instant();
        if (registro.bloqueadoHasta != null) {
            if (ahora.isBefore(registro.bloqueadoHasta)) {
                long minutos = Math.max(1, Duration.between(ahora, registro.bloqueadoHasta).toMinutes());
                throw new BusinessException(
                        "Demasiados intentos fallidos. Probá de nuevo en %d minutos.".formatted(minutos));
            }
            // El castigo ya venció: se olvida todo y arranca de cero.
            registros.remove(correo);
        }
    }

    public void registrarFallo(String correo) {
        registros.compute(correo, (clave, registro) -> {
            int fallos = (registro == null ? 0 : registro.fallos) + 1;
            Instant hasta = fallos >= MAX_FALLOS ? clock.instant().plus(BLOQUEO) : null;
            return new Registro(fallos, hasta);
        });
    }

    public void registrarExito(String correo) {
        registros.remove(correo);
    }

    private record Registro(int fallos, Instant bloqueadoHasta) {}
}
