package com.coffeecommits.brakket.auth.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Freno de fuerza bruta del login local: 5 fallos por correo → 15 minutos
 * de bloqueo. El reloj es fijo y se "avanza" recreando el Clock, para no
 * depender del tiempo real.
 */
class IntentosLoginServiceTest {

    private static final String CORREO = "ana@brakket.gg";
    private static final Instant AHORA = Instant.parse("2026-08-13T20:00:00Z");

    private MutableClock reloj;
    private IntentosLoginService service;

    @BeforeEach
    void setUp() {
        reloj = new MutableClock(AHORA);
        service = new IntentosLoginService(reloj);
    }

    @Test
    void cuatroFallosTodaviaDejanIntentar() {
        for (int i = 0; i < 4; i++) {
            service.registrarFallo(CORREO);
        }

        assertThatCode(() -> service.verificarBloqueo(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void elQuintoFalloBloqueaConElTiempoRestanteEnElMensaje() {
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(CORREO);
        }

        assertThatThrownBy(() -> service.verificarBloqueo(CORREO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Demasiados intentos")
                .hasMessageContaining("15 minutos");
    }

    @Test
    void elBloqueoVenceYArrancaDeCero() {
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(CORREO);
        }
        reloj.avanzar(Duration.ofMinutes(16));

        assertThatCode(() -> service.verificarBloqueo(CORREO)).doesNotThrowAnyException();

        // Y el contador quedó en cero: un fallo nuevo no re-bloquea.
        service.registrarFallo(CORREO);
        assertThatCode(() -> service.verificarBloqueo(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void unLoginExitosoLimpiaLosFallosAcumulados() {
        for (int i = 0; i < 4; i++) {
            service.registrarFallo(CORREO);
        }
        service.registrarExito(CORREO);
        service.registrarFallo(CORREO);

        assertThatCode(() -> service.verificarBloqueo(CORREO)).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoEsPorCorreoNoGlobal() {
        for (int i = 0; i < 5; i++) {
            service.registrarFallo(CORREO);
        }

        assertThatCode(() -> service.verificarBloqueo("otro@brakket.gg"))
                .doesNotThrowAnyException();
    }

    /** Clock de prueba: hora fija que los tests avanzan a mano. */
    private static final class MutableClock extends Clock {
        private Instant instante;

        private MutableClock(Instant inicial) { this.instante = inicial; }

        private void avanzar(Duration cuanto) { instante = instante.plus(cuanto); }

        @Override public Instant instant() { return instante; }
        @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(java.time.ZoneId zona) { return this; }
    }
}
