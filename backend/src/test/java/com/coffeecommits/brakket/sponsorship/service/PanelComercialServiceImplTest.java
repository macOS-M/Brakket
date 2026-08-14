package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.analytics.repository.AnalisisSentimientoRepository;
import com.coffeecommits.brakket.auth.dto.UsuarioResponse;
import com.coffeecommits.brakket.auth.service.AuthService;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.sponsorship.dto.MetricasPatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.dto.PanelComercialResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.EspacioPublicitarioRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import static org.mockito.Mockito.lenient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PanelComercialServiceImplTest {

    @Mock
    private AuthService authService;
    @Mock
    private PatrocinadorRepository patrocinadorRepository;
    @Mock
    private PatrocinioRepository patrocinioRepository;
    @Mock
    private EspacioPublicitarioRepository espacioRepository;
    @Mock
    private TransmisionTwitchRepository transmisionRepository;
    @Mock
    private MetricaAudienciaRepository metricaAudienciaRepository;
    @Mock
    private MetricaChatRepository metricaChatRepository;
    @Mock
    private AnalisisSentimientoRepository sentimientoRepository;
    @Mock
    private Authentication authentication;

    @InjectMocks
    private PanelComercialServiceImpl service;

    private Patrocinador patrocinador;
    private UsuarioResponse usuarioResponse;

    @BeforeEach
    void setUp() {
        patrocinador = Patrocinador.builder()
                .id(11L)
                .nombre("Nike Demo")
                .estado("ACTIVO")
                .usuarioId(1L)
                .build();

        usuarioResponse = new UsuarioResponse(
                true, 1L, "Matias", "mcalvoe@ucenfotec.ac.cr", null, null, null,
                "PUBLIC", Collections.emptyList(), Collections.emptyList(),
                null, null, null, null, null, null, null, null);

        when(authentication.getName()).thenReturn("mcalvoe@ucenfotec.ac.cr");
        when(authService.getCurrentUser("mcalvoe@ucenfotec.ac.cr")).thenReturn(usuarioResponse);
    }

    private AnalisisSentimientoRepository.ConteoClasificacion conteoDe(String clasificacion, long cantidad) {
        AnalisisSentimientoRepository.ConteoClasificacion conteo =
                mock(AnalisisSentimientoRepository.ConteoClasificacion.class);
        lenient().when(conteo.getClasificacion()).thenReturn(clasificacion);
        lenient().when(conteo.getCantidad()).thenReturn(Long.valueOf(cantidad));
        return conteo;
    }

    @Test
    void obtenerResumen_devuelve_los_patrocinios_de_la_marca() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(12L).build();
        Patrocinio patrocinio = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .estado("ACTIVO")
                .torneo(torneo)
                .fechaInicio(LocalDate.of(2026, 8, 5))
                .fechaFin(LocalDate.of(2026, 12, 31))
                .build();

        when(patrocinioRepository.findByPatrocinadorId(11L)).thenReturn(List.of(patrocinio));
        when(espacioRepository.countByPatrocinioId(2L)).thenReturn(0L);

        PanelComercialResponse response = service.obtenerResumen(authentication);

        assertThat(response.patrocinadorId()).isEqualTo(11L);
        assertThat(response.patrocinios()).hasSize(1);
        assertThat(response.patrocinios().get(0).vencido()).isFalse();
        assertThat(response.patrocinios().get(0).torneoId()).isEqualTo(12L);
    }

    @Test
    void obtenerResumen_marca_como_vencido_un_patrocinio_con_fecha_pasada() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(11L).build();
        Patrocinio patrocinioVencido = Patrocinio.builder()
                .id(3L)
                .patrocinador(patrocinador)
                .estado("ACTIVO")
                .torneo(torneo)
                .fechaInicio(LocalDate.of(2026, 7, 24))
                .fechaFin(LocalDate.of(2026, 7, 24))
                .build();

        when(patrocinioRepository.findByPatrocinadorId(11L)).thenReturn(List.of(patrocinioVencido));
        when(espacioRepository.countByPatrocinioId(3L)).thenReturn(0L);

        PanelComercialResponse response = service.obtenerResumen(authentication);

        assertThat(response.patrocinios().get(0).vencido()).isTrue();
    }

    @Test
    void obtenerResumen_falla_si_el_usuario_no_tiene_patrocinador_vinculado() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerResumen(authentication))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("no está vinculada");
    }

    @Test
    void obtenerMetricas_falla_si_el_patrocinio_no_existe() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));
        when(patrocinioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.obtenerMetricas(authentication, 999L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void obtenerMetricas_falla_si_el_patrocinio_no_pertenece_al_patrocinador_autenticado() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Patrocinador otraMarca = Patrocinador.builder().id(99L).nombre("Otra Marca").estado("ACTIVO").build();
        Patrocinio patrocinioAjeno = Patrocinio.builder()
                .id(5L)
                .patrocinador(otraMarca)
                .build();
        when(patrocinioRepository.findById(5L)).thenReturn(Optional.of(patrocinioAjeno));

        assertThatThrownBy(() -> service.obtenerMetricas(authentication, 5L))
                .isInstanceOf(ForbiddenException.class)
                .hasMessageContaining("No tenés permiso");
    }

    @Test
    void obtenerMetricas_devuelve_sin_datos_si_el_patrocinio_no_tiene_torneo_directo() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Patrocinio patrocinioDeLiga = Patrocinio.builder()
                .id(1L)
                .patrocinador(patrocinador)
                .torneo(null)
                .build();
        when(patrocinioRepository.findById(1L)).thenReturn(Optional.of(patrocinioDeLiga));

        MetricasPatrocinioResponse response = service.obtenerMetricas(authentication, 1L);

        assertThat(response.transmisionId()).isNull();
        assertThat(response.sentimientoPendiente()).isTrue();
    }

    @Test
    void obtenerMetricas_devuelve_sin_datos_si_el_torneo_no_tiene_transmision_registrada() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(12L).build();
        Patrocinio patrocinioConTorneo = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .torneo(torneo)
                .build();
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioConTorneo));
        when(transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(12L)).thenReturn(Collections.emptyList());

        MetricasPatrocinioResponse response = service.obtenerMetricas(authentication, 2L);

        assertThat(response.transmisionId()).isNull();
        assertThat(response.sentimientoPendiente()).isTrue();
    }

    @Test
    void obtenerMetricas_calcula_sentimiento_pendiente_cuando_no_hay_analisis() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(12L).build();
        Patrocinio patrocinioConTorneo = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .torneo(torneo)
                .build();
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioConTorneo));

        TransmisionTwitch transmision = TransmisionTwitch.builder().id(20L).finalizadaEn(null).build();
        when(transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(12L)).thenReturn(List.of(transmision));

        when(metricaAudienciaRepository.resumenPorTransmision(20L)).thenReturn(null);
        when(metricaChatRepository.resumenPorTransmision(20L)).thenReturn(null);
        when(sentimientoRepository.contarPorClasificacionDeTransmision(20L)).thenReturn(Collections.emptyList());

        MetricasPatrocinioResponse response = service.obtenerMetricas(authentication, 2L);

        assertThat(response.transmisionId()).isEqualTo(20L);
        assertThat(response.sentimientoPendiente()).isTrue();
        assertThat(response.sentimientoPredominante()).isNull();
    }

    @Test
    void obtenerMetricas_calcula_el_sentimiento_predominante_cuando_hay_analisis() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(12L).build();
        Patrocinio patrocinioConTorneo = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .torneo(torneo)
                .build();
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioConTorneo));

        TransmisionTwitch transmision = TransmisionTwitch.builder().id(20L).finalizadaEn(null).build();
        when(transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(12L)).thenReturn(List.of(transmision));

        List<AnalisisSentimientoRepository.ConteoClasificacion> conteos =
                List.of(conteoDe("POSITIVO", 2L), conteoDe("NEGATIVO", 1L));
        when(sentimientoRepository.contarPorClasificacionDeTransmision(20L)).thenReturn(conteos);

        when(metricaAudienciaRepository.resumenPorTransmision(20L)).thenReturn(null);
        when(metricaChatRepository.resumenPorTransmision(20L)).thenReturn(null);

        MetricasPatrocinioResponse response = service.obtenerMetricas(authentication, 2L);

        assertThat(response.sentimientoPendiente()).isFalse();
        assertThat(response.sentimientoPredominante()).isEqualTo("POSITIVO");
    }

    @Test
    void obtenerMetricas_elige_la_transmision_en_vivo_cuando_hay_varias() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(27L).build();
        Patrocinio patrocinioConTorneo = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .torneo(torneo)
                .build();
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioConTorneo));

        TransmisionTwitch finalizada = TransmisionTwitch.builder()
                .id(21L)
                .finalizadaEn(LocalDateTime.of(2026, 8, 1, 20, 0))
                .build();
        TransmisionTwitch enVivo = TransmisionTwitch.builder()
                .id(22L)
                .finalizadaEn(null)
                .build();

        when(transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(27L))
                .thenReturn(List.of(finalizada, enVivo));
        when(metricaAudienciaRepository.resumenPorTransmision(22L)).thenReturn(null);
        when(metricaChatRepository.resumenPorTransmision(22L)).thenReturn(null);
        when(sentimientoRepository.contarPorClasificacionDeTransmision(22L)).thenReturn(Collections.emptyList());

        MetricasPatrocinioResponse response = service.obtenerMetricas(authentication, 2L);

        assertThat(response.transmisionId()).isEqualTo(22L);
    }

    @Test
    void obtenerMetricas_elige_la_mas_reciente_cuando_ninguna_esta_en_vivo() {
        when(patrocinadorRepository.findByUsuarioId(1L)).thenReturn(Optional.of(patrocinador));

        Torneo torneo = Torneo.builder().id(27L).build();
        Patrocinio patrocinioConTorneo = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .torneo(torneo)
                .build();
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioConTorneo));

        TransmisionTwitch masReciente = TransmisionTwitch.builder()
                .id(23L)
                .finalizadaEn(LocalDateTime.of(2026, 8, 8, 20, 0))
                .build();
        TransmisionTwitch masVieja = TransmisionTwitch.builder()
                .id(21L)
                .finalizadaEn(LocalDateTime.of(2026, 8, 1, 20, 0))
                .build();

        when(transmisionRepository.findByTorneoIdOrderByIniciadaEnDesc(27L))
                .thenReturn(List.of(masReciente, masVieja));
        when(metricaAudienciaRepository.resumenPorTransmision(23L)).thenReturn(null);
        when(metricaChatRepository.resumenPorTransmision(23L)).thenReturn(null);
        when(sentimientoRepository.contarPorClasificacionDeTransmision(23L)).thenReturn(Collections.emptyList());

        MetricasPatrocinioResponse response = service.obtenerMetricas(authentication, 2L);

        assertThat(response.transmisionId()).isEqualTo(23L);
    }
}