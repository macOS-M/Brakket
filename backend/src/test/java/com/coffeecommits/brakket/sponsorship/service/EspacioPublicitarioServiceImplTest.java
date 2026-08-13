package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.sponsorship.dto.CrearEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EspacioPublicitarioResponse;
import com.coffeecommits.brakket.sponsorship.model.EspacioPublicitario;
import com.coffeecommits.brakket.sponsorship.model.Patrocinador;
import com.coffeecommits.brakket.sponsorship.model.Patrocinio;
import com.coffeecommits.brakket.sponsorship.repository.EspacioPublicitarioRepository;
import com.coffeecommits.brakket.sponsorship.repository.PatrocinioRepository;
import com.coffeecommits.brakket.tournament.model.Torneo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EspacioPublicitarioServiceImplTest {

    @Mock
    private EspacioPublicitarioRepository espacioRepository;
    @Mock
    private PatrocinioRepository patrocinioRepository;

    @InjectMocks
    private EspacioPublicitarioServiceImpl service;

    private Patrocinio patrocinioActivo;

    @BeforeEach
    void setUp() {
        Patrocinador patrocinador = Patrocinador.builder()
                .id(11L)
                .nombre("Nike Demo")
                .estado("ACTIVO")
                .build();

        // Sin liga ni torneo seteados a propósito: cubre la rama sin
        // restricción de alcance (ver ubicacionesPermitidasPara), y mantiene
        // las pruebas ya existentes de este archivo sin cambios de comportamiento.
        patrocinioActivo = Patrocinio.builder()
                .id(2L)
                .patrocinador(patrocinador)
                .estado("ACTIVO")
                .fechaInicio(LocalDate.of(2026, 8, 5))
                .fechaFin(LocalDate.of(2026, 12, 31))
                .build();
    }

    private CrearEspacioPublicitarioRequest requestBasico(String ubicacion) {
        return new CrearEspacioPublicitarioRequest(
                2L, ubicacion, "https://ejemplo.com/banner.png", "https://nike.com");
    }

    @Test
    void crear_registra_el_espacio_correctamente() {
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));
        when(espacioRepository.existeEspacioOcupado(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(espacioRepository.save(any(EspacioPublicitario.class))).thenAnswer(inv -> {
            EspacioPublicitario e = inv.getArgument(0);
            e.setId(1L);
            return e;
        });

        EspacioPublicitarioResponse response = service.crear(requestBasico("TORNEO_CABECERA"));

        assertThat(response.id()).isEqualTo(1L);
        assertThat(response.ubicacion()).isEqualTo("TORNEO_CABECERA");
        assertThat(response.estado()).isEqualTo("ACTIVO");

        ArgumentCaptor<EspacioPublicitario> captor = ArgumentCaptor.forClass(EspacioPublicitario.class);
        verify(espacioRepository).save(captor.capture());
        assertThat(captor.getValue().getPatrocinio()).isEqualTo(patrocinioActivo);
    }

    @Test
    void crear_falla_si_el_patrocinio_no_existe() {
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.crear(requestBasico("TORNEO_CABECERA")))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_el_patrocinio_no_esta_activo() {
        patrocinioActivo.setEstado("CANCELADO");
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));

        assertThatThrownBy(() -> service.crear(requestBasico("TORNEO_CABECERA")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("activo");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void crear_falla_si_la_ubicacion_no_es_valida() {
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));

        assertThatThrownBy(() -> service.crear(requestBasico("BANNER_LATERAL")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ubicación debe ser una de");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void crear_normaliza_la_ubicacion_a_mayusculas() {
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));
        when(espacioRepository.existeEspacioOcupado(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(espacioRepository.save(any(EspacioPublicitario.class))).thenAnswer(inv -> inv.getArgument(0));

        EspacioPublicitarioResponse response = service.crear(requestBasico("torneo_cabecera"));

        assertThat(response.ubicacion()).isEqualTo("TORNEO_CABECERA");
    }

    @Test
    void crear_falla_si_el_espacio_ya_esta_ocupado_para_el_periodo() {
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));
        when(espacioRepository.existeEspacioOcupado(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(true);

        assertThatThrownBy(() -> service.crear(requestBasico("TORNEO_CABECERA")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está ocupado");
        verify(espacioRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Nuevo: el alcance del patrocinio restringe qué ubicaciones puede
    // reclamar (fix del caso reportado: un patrocinio de un torneo puntual
    // no debe poder ponerse en la cabecera de toda la liga).
    // ---------------------------------------------------------------

    @Test
    void crear_falla_si_un_patrocinio_de_torneo_intenta_reclamar_liga_cabecera() {
        Torneo torneo = Torneo.builder().id(3L).build();
        patrocinioActivo.setTorneo(torneo);
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));

        assertThatThrownBy(() -> service.crear(requestBasico("LIGA_CABECERA")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no corresponde al alcance de este patrocinio");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void crear_permite_transmision_inferior_para_un_patrocinio_de_torneo() {
        Torneo torneo = Torneo.builder().id(3L).build();
        patrocinioActivo.setTorneo(torneo);
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));
        when(espacioRepository.existeEspacioOcupado(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(false);
        when(espacioRepository.save(any(EspacioPublicitario.class))).thenAnswer(inv -> inv.getArgument(0));

        EspacioPublicitarioResponse response = service.crear(requestBasico("TRANSMISION_INFERIOR"));

        assertThat(response.ubicacion()).isEqualTo("TRANSMISION_INFERIOR");
    }

    @Test
    void crear_falla_si_un_patrocinio_de_liga_intenta_reclamar_torneo_cabecera() {
        Liga liga = Liga.builder().id(8L).build();
        patrocinioActivo.setLiga(liga);
        when(patrocinioRepository.findById(2L)).thenReturn(Optional.of(patrocinioActivo));

        assertThatThrownBy(() -> service.crear(requestBasico("TORNEO_CABECERA")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no corresponde al alcance de este patrocinio");
        verify(espacioRepository, never()).save(any());
    }
}