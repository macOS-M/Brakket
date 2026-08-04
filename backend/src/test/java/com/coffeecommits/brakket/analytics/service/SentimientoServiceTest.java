package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.AnalizarChatRequest;
import com.coffeecommits.brakket.analytics.dto.SentimientoResponse;
import com.coffeecommits.brakket.analytics.dto.SerieSentimientoResponse;
import com.coffeecommits.brakket.analytics.model.AnalisisSentimiento;
import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SentimientoServiceTest {

    @Mock
    private AnalisisSentimientoRepository analisisRepository;
    @Mock
    private MetricaChatRepository metricaChatRepository;
    @Mock
    private TransmisionTwitchRepository transmisionRepository;

    private SentimientoService service;

    private static final Long TRANSMISION_ID = 7L;

    @BeforeEach
    void setUp() {
        // Analizador real (determinista): así el test cubre servicio + motor juntos.
        service = new SentimientoService(analisisRepository, metricaChatRepository,
                transmisionRepository, new AnalizadorLexico());
    }

    private TransmisionTwitch transmision() {
        return TransmisionTwitch.builder().id(TRANSMISION_ID).build();
    }

    @Test
    void analizar_persiste_muestra_y_analisis_y_clasifica_positivo() {
        when(transmisionRepository.findById(TRANSMISION_ID)).thenReturn(Optional.of(transmision()));
        when(metricaChatRepository.save(any(MetricaChat.class))).thenAnswer(inv -> {
            MetricaChat m = inv.getArgument(0);
            m.setId(30L);
            return m;
        });
        when(analisisRepository.save(any(AnalisisSentimiento.class))).thenAnswer(inv -> {
            AnalisisSentimiento a = inv.getArgument(0);
            a.setId(90L);
            return a;
        });

        SentimientoResponse resp = service.analizar(TRANSMISION_ID,
                new AnalizarChatRequest(List.of("GG jugada increíble", "pog vamos", "clutch 🔥"), null));

        assertThat(resp.id()).isEqualTo(90L);
        assertThat(resp.transmisionId()).isEqualTo(TRANSMISION_ID);
        assertThat(resp.clasificacion()).isEqualTo(ClasificacionSentimiento.POSITIVO.name());
        assertThat(resp.puntaje()).isEqualByComparingTo("100.00");
        assertThat(resp.mensajesAnalizados()).isEqualTo(3);
        assertThat(resp.usuariosActivos()).isEqualTo(3); // sin dato ⇒ se aproxima con los mensajes
        verify(metricaChatRepository).save(any(MetricaChat.class));
        verify(analisisRepository).save(any(AnalisisSentimiento.class));
    }

    @Test
    void analizar_usa_usuarios_activos_informados() {
        when(transmisionRepository.findById(TRANSMISION_ID)).thenReturn(Optional.of(transmision()));
        when(metricaChatRepository.save(any(MetricaChat.class))).thenAnswer(inv -> inv.getArgument(0));
        when(analisisRepository.save(any(AnalisisSentimiento.class))).thenAnswer(inv -> inv.getArgument(0));

        SentimientoResponse resp = service.analizar(TRANSMISION_ID,
                new AnalizarChatRequest(List.of("gg", "gg"), 15));

        assertThat(resp.usuariosActivos()).isEqualTo(15);
        assertThat(resp.mensajesAnalizados()).isEqualTo(2);
    }

    @Test
    void analizar_ignora_mensajes_vacios_y_falla_si_no_queda_ninguno() {
        when(transmisionRepository.findById(TRANSMISION_ID)).thenReturn(Optional.of(transmision()));

        AnalizarChatRequest request = new AnalizarChatRequest(java.util.Arrays.asList("", "   ", null), null);

        assertThatThrownBy(() -> service.analizar(TRANSMISION_ID, request))
                .isInstanceOf(BusinessException.class);
        verify(metricaChatRepository, never()).save(any());
        verify(analisisRepository, never()).save(any());
    }

    @Test
    void analizar_lanza_404_si_la_transmision_no_existe() {
        when(transmisionRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.analizar(99L,
                new AnalizarChatRequest(List.of("gg"), null)))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void serie_calcula_promedio_ultimo_y_puntos() {
        when(transmisionRepository.findById(TRANSMISION_ID)).thenReturn(Optional.of(transmision()));
        when(analisisRepository.findSerieByTransmision(TRANSMISION_ID))
                .thenReturn(List.of(analisis(1L, "80.00"), analisis(2L, "-20.00")));

        SerieSentimientoResponse serie = service.serie(TRANSMISION_ID);

        assertThat(serie.totalMuestras()).isEqualTo(2);
        assertThat(serie.puntos()).hasSize(2);
        assertThat(serie.promedioPuntaje()).isEqualByComparingTo("30.00"); // (80 - 20) / 2
        assertThat(serie.ultimo().puntaje()).isEqualByComparingTo("-20.00");
    }

    @Test
    void serie_sin_muestras_devuelve_vacia() {
        when(transmisionRepository.findById(TRANSMISION_ID)).thenReturn(Optional.of(transmision()));
        when(analisisRepository.findSerieByTransmision(TRANSMISION_ID)).thenReturn(List.of());

        SerieSentimientoResponse serie = service.serie(TRANSMISION_ID);

        assertThat(serie.totalMuestras()).isZero();
        assertThat(serie.ultimo()).isNull();
        assertThat(serie.promedioPuntaje()).isNull();
        assertThat(serie.puntos()).isEmpty();
    }

    private AnalisisSentimiento analisis(Long id, String puntaje) {
        MetricaChat metrica = MetricaChat.builder()
                .id(id).transmisionTwitch(transmision())
                .fechaHora(LocalDateTime.now()).mensajesPorMinuto(5).usuariosActivos(4).build();
        return AnalisisSentimiento.builder()
                .id(id).metricaChat(metrica).fechaHora(LocalDateTime.now())
                .clasificacion(ClasificacionSentimiento.NEUTRO.name())
                .puntaje(new BigDecimal(puntaje)).build();
    }
}
