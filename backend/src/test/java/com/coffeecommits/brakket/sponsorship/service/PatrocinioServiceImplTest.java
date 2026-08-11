package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.league.model.Temporada;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinioRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinadorRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PatrocinioServiceImplTest {

    @Mock
    private PatrocinioRepository patrocinioRepository;
    @Mock
    private PatrocinadorRepository patrocinadorRepository;
    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private TemporadaRepository temporadaRepository;
    @Mock
    private TorneoRepository torneoRepository;

    @InjectMocks
    private PatrocinioServiceImpl service;

    private Patrocinador patrocinadorActivo;
    private Torneo torneoSinFechaFin;

    @BeforeEach
    void setUp() {
        patrocinadorActivo = Patrocinador.builder()
                .id(11L)
                .nombre("Nike Demo")
                .estado("ACTIVO")
                .build();

        torneoSinFechaFin = Torneo.builder()
                .id(12L)
                .nombre("Copa Nocturna 5v5")
                .fechaInicio(LocalDateTime.of(2026, 7, 26, 4, 35))
                .fechaFin(null)
                .build();
    }

    private CrearPatrocinioRequest requestParaTorneo(Long torneoId, String nivel,
                                                     LocalDate fechaInicio, LocalDate fechaFin) {
        return new CrearPatrocinioRequest(
                11L, null, null, torneoId, nivel, "Condiciones de prueba", fechaInicio, fechaFin);
    }

    @Test
    void crear_asocia_patrocinio_a_torneo_correctamente() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));
        when(torneoRepository.findById(12L)).thenReturn(Optional.of(torneoSinFechaFin));
        when(patrocinioRepository.existeSolapamiento(any(), any(), any(), anyString(), any(), any()))
                .thenReturn(false);
        when(patrocinioRepository.save(any(Patrocinio.class))).thenAnswer(inv -> {
            Patrocinio p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        CrearPatrocinioRequest request = requestParaTorneo(
                12L, "ORO", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 12, 31));

        PatrocinioResponse response = service.crear(request);

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.patrocinadorId()).isEqualTo(11L);
        assertThat(response.torneoId()).isEqualTo(12L);
        assertThat(response.nivel()).isEqualTo("ORO");
        assertThat(response.estado()).isEqualTo("ACTIVO");

        ArgumentCaptor<Patrocinio> captor = ArgumentCaptor.forClass(Patrocinio.class);
        verify(patrocinioRepository).save(captor.capture());
        assertThat(captor.getValue().getPatrocinador()).isEqualTo(patrocinadorActivo);
        assertThat(captor.getValue().getTorneo()).isEqualTo(torneoSinFechaFin);
    }

    @Test
    void crear_permite_dos_niveles_distintos_en_la_misma_competencia_y_periodo() {
        // Caso que estaba roto: ORO y PLATA deben poder coexistir en el mismo
        // torneo y periodo. existeSolapamiento debe filtrar tambien por nivel,
        // asi que devolver false aqui (nadie mas tiene nivel PLATA en ese
        // torneo) confirma que la creacion no se bloquea.
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));
        when(torneoRepository.findById(12L)).thenReturn(Optional.of(torneoSinFechaFin));
        when(patrocinioRepository.existeSolapamiento(any(), any(), any(), anyString(), any(), any()))
                .thenReturn(false);
        when(patrocinioRepository.save(any(Patrocinio.class))).thenAnswer(inv -> inv.getArgument(0));

        CrearPatrocinioRequest requestPlata = requestParaTorneo(
                12L, "PLATA", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 12, 31));

        PatrocinioResponse response = service.crear(requestPlata);

        assertThat(response.nivel()).isEqualTo("PLATA");
        verify(patrocinioRepository).existeSolapamiento(
                any(), any(), any(), org.mockito.ArgumentMatchers.eq("PLATA"), any(), any());
    }

    @Test
    void crear_falla_si_el_patrocinador_esta_inactivo() {
        Patrocinador inactivo = Patrocinador.builder().id(11L).nombre("Nike Demo").estado("INACTIVO").build();
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(inactivo));

        CrearPatrocinioRequest request = requestParaTorneo(
                12L, "ORO", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("activo");
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_el_patrocinador_no_existe() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.empty());

        CrearPatrocinioRequest request = requestParaTorneo(
                12L, "ORO", LocalDate.of(2026, 8, 5), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_se_envian_dos_alcances_a_la_vez() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));

        CrearPatrocinioRequest request = new CrearPatrocinioRequest(
                11L, 1L, null, 12L, "ORO", null,
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exactamente una competencia");
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_no_se_envia_ningun_alcance() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));

        CrearPatrocinioRequest request = new CrearPatrocinioRequest(
                11L, null, null, null, "ORO", null,
                LocalDate.of(2026, 8, 5), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("exactamente una competencia");
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_la_fecha_de_inicio_es_posterior_a_la_fecha_de_fin() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));

        CrearPatrocinioRequest request = requestParaTorneo(
                12L, "ORO", LocalDate.of(2026, 12, 31), LocalDate.of(2026, 8, 5));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("fecha de inicio no puede ser posterior");
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_la_fecha_de_inicio_es_anterior_al_inicio_del_torneo() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));
        when(torneoRepository.findById(12L)).thenReturn(Optional.of(torneoSinFechaFin));

        CrearPatrocinioRequest request = requestParaTorneo(
                12L, "ORO", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 12, 31));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("anterior al inicio de la competencia");
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_la_fecha_fin_es_posterior_al_fin_de_una_temporada() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));
        Temporada temporada = Temporada.builder()
                .id(5L)
                .fechaInicio(LocalDate.of(2026, 1, 1))
                .fechaFin(LocalDate.of(2026, 6, 30))
                .build();
        when(temporadaRepository.findById(5L)).thenReturn(Optional.of(temporada));

        CrearPatrocinioRequest request = new CrearPatrocinioRequest(
                11L, null, 5L, null, "ORO", null,
                LocalDate.of(2026, 3, 1), LocalDate.of(2026, 9, 1));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("posterior al fin de la competencia");
        verify(patrocinioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_existe_solapamiento_con_el_mismo_nivel_en_la_misma_competencia() {
        when(patrocinadorRepository.findById(11L)).thenReturn(Optional.of(patrocinadorActivo));
        when(torneoRepository.findById(12L)).thenReturn(Optional.of(torneoSinFechaFin));
        when(patrocinioRepository.existeSolapamiento(any(), any(), any(), anyString(), any(), any()))
                .thenReturn(true);

        CrearPatrocinioRequest request = requestParaTorneo(
                12L, "ORO", LocalDate.of(2026, 9, 1), LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> service.crear(request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya existe un patrocinio de nivel ORO");
        verify(patrocinioRepository, never()).save(any());
    }
}