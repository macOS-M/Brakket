package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.TermometroResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.analytics.model.EstadoTermometro;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.config.AnalyticsProperties;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RF-40: el termómetro lee lo que RF-39 dejó persistido y lo convierte en algo
 * interpretable. Se cubren los tres estados que distingue la ERS —pendiente,
 * datos insuficientes y disponible—, la agrupación por intervalos y la
 * validación del período.
 */
@ExtendWith(MockitoExtension.class)
class TermometroServiceTest {

    private static final Long TRANSMISION = 7L;
    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 10, 20, 0);

    @Mock
    private AnalisisSentimientoRepository analisisRepository;

    @Mock
    private TransmisionTwitchRepository transmisionRepository;

    private TermometroService service;

    @BeforeEach
    void setUp() {
        service = new TermometroService(analisisRepository, transmisionRepository,
                new AnalyticsProperties());
    }

    /** Una muestra al minuto {@code minuto} con el puntaje indicado. */
    private static AnalisisSentimiento muestra(int minuto, String puntaje) {
        BigDecimal valor = new BigDecimal(puntaje);
        return AnalisisSentimiento.builder()
                .fechaHora(BASE.plusMinutes(minuto))
                .puntaje(valor)
                .clasificacion(ClasificacionSentimiento.desdePuntaje(valor).name())
                .build();
    }

    private void laTransmisionExiste() {
        when(transmisionRepository.existsById(TRANSMISION)).thenReturn(true);
    }

    private void laSerieEs(AnalisisSentimiento... muestras) {
        when(analisisRepository.findSerieByTransmisionEntre(eq(TRANSMISION), any(), any()))
                .thenReturn(Arrays.asList(muestras));
    }

    private TermometroResponse consultar() {
        return service.consultar(TRANSMISION, null, null, null);
    }

    @Test
    void una_transmision_inexistente_es_404() {
        when(transmisionRepository.existsById(TRANSMISION)).thenReturn(false);

        assertThatThrownBy(this::consultar).isInstanceOf(ResourceNotFoundException.class);
        verify(analisisRepository, never()).findSerieByTransmisionEntre(any(), any(), any());
    }

    @Test
    void sin_analisis_el_estado_es_pendiente() {
        laTransmisionExiste();
        laSerieEs();

        TermometroResponse termometro = consultar();

        assertThat(termometro.estado()).isEqualTo(EstadoTermometro.PENDIENTE);
        // La ERS pide mostrar el estado, no un indicador vacio que la vista
        // pueda confundir con "chat neutro".
        assertThat(termometro.puntajeGeneral()).isNull();
        assertThat(termometro.clasificacion()).isNull();
        assertThat(termometro.resumen()).contains("Todavía no hay análisis");
    }

    @Test
    void con_menos_muestras_que_el_minimo_el_estado_es_insuficiente() {
        laTransmisionExiste();
        laSerieEs(muestra(0, "80"), muestra(1, "90")); // el minimo por defecto es 3

        TermometroResponse termometro = consultar();

        assertThat(termometro.estado()).isEqualTo(EstadoTermometro.INSUFICIENTE);
        assertThat(termometro.puntajeGeneral()).isNull();
        assertThat(termometro.totalMuestras()).isEqualTo(2);
        assertThat(termometro.minimoMuestras()).isEqualTo(3);
        assertThat(termometro.resumen()).contains("se necesitan al menos 3");
        // Aunque el indicador se oculte, el detalle viaja: la vista puede
        // mostrar cuantas muestras hay sin inventar un puntaje.
        assertThat(termometro.distribucion().positivo()).isEqualTo(2);
    }

    @Test
    void con_muestras_suficientes_promedia_y_clasifica() {
        laTransmisionExiste();
        laSerieEs(muestra(0, "60"), muestra(1, "30"), muestra(2, "0"));

        TermometroResponse termometro = consultar();

        assertThat(termometro.estado()).isEqualTo(EstadoTermometro.DISPONIBLE);
        assertThat(termometro.puntajeGeneral()).isEqualByComparingTo("30.00");
        assertThat(termometro.clasificacion()).isEqualTo("POSITIVO");
        assertThat(termometro.totalMuestras()).isEqualTo(3);
    }

    @Test
    void la_distribucion_cuenta_las_tres_clasificaciones_con_sus_porcentajes() {
        laTransmisionExiste();
        laSerieEs(muestra(0, "80"), muestra(1, "0"), muestra(2, "-80"), muestra(3, "90"));

        var distribucion = consultar().distribucion();

        assertThat(distribucion.positivo()).isEqualTo(2);
        assertThat(distribucion.neutro()).isEqualTo(1);
        assertThat(distribucion.negativo()).isEqualTo(1);
        assertThat(distribucion.porcentajePositivo()).isEqualByComparingTo("50.00");
        assertThat(distribucion.porcentajeNeutro()).isEqualByComparingTo("25.00");
    }

    @Test
    void un_chat_partido_no_se_confunde_con_uno_tibio() {
        laTransmisionExiste();
        // Mitad eufórico y mitad hostil promedia 0, igual que un chat neutro.
        laSerieEs(muestra(0, "100"), muestra(1, "100"), muestra(2, "-100"), muestra(3, "-100"));

        TermometroResponse termometro = consultar();

        assertThat(termometro.puntajeGeneral()).isEqualByComparingTo("0.00");
        assertThat(termometro.clasificacion()).isEqualTo("NEUTRO");
        // Para eso esta la distribucion: el promedio solo no lo distingue.
        assertThat(termometro.distribucion().neutro()).isZero();
        assertThat(termometro.distribucion().positivo()).isEqualTo(2);
        assertThat(termometro.distribucion().negativo()).isEqualTo(2);
    }

    @Test
    void agrupa_las_muestras_en_intervalos_del_ancho_pedido() {
        laTransmisionExiste();
        // Cinco minutos de muestras agrupadas de a dos: 0-1, 2-3 y 4.
        laSerieEs(muestra(0, "10"), muestra(1, "30"),
                muestra(2, "-40"), muestra(3, "-60"),
                muestra(4, "50"));

        var intervalos = service.consultar(TRANSMISION, null, null, 2).intervalos();

        assertThat(intervalos).hasSize(3);
        assertThat(intervalos.get(0).puntajePromedio()).isEqualByComparingTo("20.00");
        assertThat(intervalos.get(0).muestras()).isEqualTo(2);
        assertThat(intervalos.get(1).puntajePromedio()).isEqualByComparingTo("-50.00");
        assertThat(intervalos.get(1).clasificacion()).isEqualTo("NEGATIVO");
        assertThat(intervalos.get(2).muestras()).isEqualTo(1);
    }

    @Test
    void los_tramos_sin_muestras_no_se_emiten() {
        laTransmisionExiste();
        // Hueco de media hora entre la segunda y la tercera muestra.
        laSerieEs(muestra(0, "50"), muestra(1, "50"), muestra(40, "50"));

        var intervalos = service.consultar(TRANSMISION, null, null, 5).intervalos();

        // Un hueco no es un cero: en esta escala, cero significa "chat neutro".
        assertThat(intervalos).hasSize(2);
        assertThat(intervalos.get(0).muestras()).isEqualTo(2);
        assertThat(intervalos.get(1).muestras()).isEqualTo(1);
    }

    @Test
    void un_periodo_invertido_se_rechaza() {
        laTransmisionExiste();

        assertThatThrownBy(() -> service.consultar(TRANSMISION, BASE.plusHours(2), BASE, null))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no puede ser posterior");
        // Se rechaza en vez de devolver vacio: un rango invertido es un filtro
        // mal armado, y "sin datos" lo haria parecer un problema de la captura.
        verify(analisisRepository, never()).findSerieByTransmisionEntre(any(), any(), any());
    }

    @Test
    void un_intervalo_no_positivo_se_rechaza() {
        laTransmisionExiste();

        assertThatThrownBy(() -> service.consultar(TRANSMISION, null, null, 0))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("al menos un minuto");
    }

    @Test
    void un_periodo_que_da_demasiados_intervalos_se_rechaza() {
        laTransmisionExiste();

        assertThatThrownBy(() -> service.consultar(TRANSMISION, BASE, BASE.plusDays(30), 1))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ampliá el intervalo");
    }

    @Test
    void un_periodo_abierto_consulta_con_cotas_que_lo_cubren_todo() {
        laTransmisionExiste();
        laSerieEs(muestra(0, "40"), muestra(1, "40"), muestra(2, "40"));

        consultar();

        // Nunca se le pasan nulos a la consulta: el JPQL no tendria como
        // inferir el tipo del parametro.
        ArgumentCaptor<LocalDateTime> desde = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hasta = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(analisisRepository)
                .findSerieByTransmisionEntre(eq(TRANSMISION), desde.capture(), hasta.capture());

        assertThat(desde.getValue()).isBefore(BASE);
        assertThat(hasta.getValue()).isAfter(BASE.plusYears(100));
    }

    @Test
    void el_periodo_pedido_se_pasa_a_la_consulta_y_vuelve_en_la_respuesta() {
        laTransmisionExiste();
        LocalDateTime hasta = BASE.plusHours(1);
        when(analisisRepository.findSerieByTransmisionEntre(TRANSMISION, BASE, hasta))
                .thenReturn(List.of(muestra(0, "40"), muestra(1, "40"), muestra(2, "40")));

        TermometroResponse termometro = service.consultar(TRANSMISION, BASE, hasta, 10);

        assertThat(termometro.desde()).isEqualTo(BASE);
        assertThat(termometro.hasta()).isEqualTo(hasta);
        assertThat(termometro.intervaloMinutos()).isEqualTo(10);
    }

    @Test
    void el_resumen_describe_el_ambiente_la_distribucion_y_la_tendencia() {
        laTransmisionExiste();
        // Promedio 35 (POSITIVO) y una segunda mitad muy por encima de la primera.
        laSerieEs(muestra(0, "-20"), muestra(1, "-10"), muestra(2, "80"), muestra(3, "90"));

        String resumen = consultar().resumen();

        assertThat(resumen).contains("positivo");
        assertThat(resumen).contains("4 muestras");
        assertThat(resumen).contains("mejorando");
        // La ERS pide presentarlo como apoyo informativo, no como una decision
        // automatica sobre usuarios.
        assertThat(resumen).contains("no una evaluación de usuarios");
    }

    @Test
    void con_pocas_muestras_el_resumen_no_afirma_una_tendencia() {
        laTransmisionExiste();
        laSerieEs(muestra(0, "10"), muestra(1, "90"), muestra(2, "95"));

        assertThat(consultar().resumen())
                .contains("no hay muestras suficientes para hablar de una tendencia");
    }
}
