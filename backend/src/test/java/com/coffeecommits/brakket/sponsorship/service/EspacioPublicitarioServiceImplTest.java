package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.league.model.Liga;
import com.coffeecommits.brakket.sponsorship.dto.CrearEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarEspacioPublicitarioRequest;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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

    // ---------------------------------------------------------------
    // Nuevo: editar y eliminar
    // ---------------------------------------------------------------

    private EspacioPublicitario espacioExistente(String ubicacion) {
        return EspacioPublicitario.builder()
                .id(9L)
                .patrocinio(patrocinioActivo)
                .ubicacion(ubicacion)
                .imagenUrl("https://ejemplo.com/viejo.png")
                .enlaceUrl(null)
                .estado("ACTIVO")
                .build();
    }

    @Test
    void editar_actualiza_imagen_y_enlace_correctamente() {
        EspacioPublicitario existente = espacioExistente("TORNEO_CABECERA");
        Torneo torneo = Torneo.builder().id(3L).build();
        patrocinioActivo.setTorneo(torneo);

        when(espacioRepository.findById(9L)).thenReturn(Optional.of(existente));
        when(espacioRepository.existeEspacioOcupado(any(), any(), any(), any(), any(), any(), eq(9L)))
                .thenReturn(false);
        when(espacioRepository.save(any(EspacioPublicitario.class))).thenAnswer(inv -> inv.getArgument(0));

        EditarEspacioPublicitarioRequest request = new EditarEspacioPublicitarioRequest(
                "TORNEO_CABECERA", "https://ejemplo.com/nuevo.png", "https://nike.com/promo");

        EspacioPublicitarioResponse response = service.editar(9L, request);

        assertThat(response.imagenUrl()).isEqualTo("https://ejemplo.com/nuevo.png");
        assertThat(response.enlaceUrl()).isEqualTo("https://nike.com/promo");
        // El excluirEspacioId (9L) debe llegar al repositorio para no chocar consigo mismo.
        verify(espacioRepository).existeEspacioOcupado(any(), any(), any(), any(), any(), any(), eq(9L));
    }

    @Test
    void editar_falla_si_el_espacio_no_existe() {
        when(espacioRepository.findById(999L)).thenReturn(Optional.empty());

        EditarEspacioPublicitarioRequest request = new EditarEspacioPublicitarioRequest(
                "TORNEO_CABECERA", "https://ejemplo.com/nuevo.png", null);

        assertThatThrownBy(() -> service.editar(999L, request))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void editar_falla_si_la_nueva_ubicacion_no_corresponde_al_alcance() {
        EspacioPublicitario existente = espacioExistente("TORNEO_CABECERA");
        Torneo torneo = Torneo.builder().id(3L).build();
        patrocinioActivo.setTorneo(torneo);
        when(espacioRepository.findById(9L)).thenReturn(Optional.of(existente));

        EditarEspacioPublicitarioRequest request = new EditarEspacioPublicitarioRequest(
                "LIGA_CABECERA", "https://ejemplo.com/nuevo.png", null);

        assertThatThrownBy(() -> service.editar(9L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no corresponde al alcance de este patrocinio");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void editar_falla_si_la_nueva_ubicacion_ya_esta_ocupada_por_otro_espacio() {
        EspacioPublicitario existente = espacioExistente("TORNEO_CABECERA");
        Torneo torneo = Torneo.builder().id(3L).build();
        patrocinioActivo.setTorneo(torneo);
        when(espacioRepository.findById(9L)).thenReturn(Optional.of(existente));
        when(espacioRepository.existeEspacioOcupado(any(), any(), any(), any(), any(), any(), eq(9L)))
                .thenReturn(true);

        EditarEspacioPublicitarioRequest request = new EditarEspacioPublicitarioRequest(
                "TRANSMISION_INFERIOR", "https://ejemplo.com/nuevo.png", null);

        assertThatThrownBy(() -> service.editar(9L, request))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está ocupado");
        verify(espacioRepository, never()).save(any());
    }

    @Test
    void eliminar_borra_el_espacio_existente() {
        EspacioPublicitario existente = espacioExistente("TORNEO_CABECERA");
        when(espacioRepository.findById(9L)).thenReturn(Optional.of(existente));

        service.eliminar(9L);

        verify(espacioRepository).delete(existente);
    }

    @Test
    void eliminar_falla_si_el_espacio_no_existe() {
        when(espacioRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.eliminar(999L))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(espacioRepository, never()).delete(any(EspacioPublicitario.class));
    }
}