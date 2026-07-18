package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.game.dto.PerfilCompetitivoRequest;
import com.coffeecommits.brakket.game.model.EstadisticaJuego;
import com.coffeecommits.brakket.game.model.FormatoCompetitivo;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.model.ModalidadCompetitiva;
import com.coffeecommits.brakket.game.model.PerfilCompetitivoJuego;
import com.coffeecommits.brakket.game.repository.EstadisticaJuegoRepository;
import com.coffeecommits.brakket.game.repository.FormatoCompetitivoRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.game.repository.PerfilCompetitivoRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompetitiveProfileServiceTest {

    @Mock PerfilCompetitivoRepository perfilRepository;
    @Mock JuegoRepository juegoRepository;
    @Mock FormatoCompetitivoRepository formatoRepository;
    @Mock EstadisticaJuegoRepository estadisticaRepository;
    @Mock TorneoRepository torneoRepository;
    @InjectMocks CompetitiveProfileService service;

    @Test
    void crear_registra_perfil_valido_y_confirma() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").activo(true).build();
        FormatoCompetitivo formato = new FormatoCompetitivo(10L, "ROUND_ROBIN", true);
        EstadisticaJuego estadistica = new EstadisticaJuego(20L, "VICTORIAS", true, true);
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(perfilRepository.findByJuegoId(1L)).thenReturn(Optional.empty());
        when(formatoRepository.findAllById(List.of(10L))).thenReturn(List.of(formato));
        when(estadisticaRepository.findAllById(List.of(20L))).thenReturn(List.of(estadistica));
        when(estadisticaRepository.findByObligatoriaTrueAndActivaTrue()).thenReturn(List.of(estadistica));
        when(perfilRepository.save(any())).thenAnswer(inv -> {
            PerfilCompetitivoJuego p = inv.getArgument(0);
            p.setId(30L);
            return p;
        });

        var response = service.crear(requestEquipos());

        assertThat(response.id()).isEqualTo(30L);
        assertThat(response.mensaje()).isEqualTo("Perfil competitivo creado correctamente");
        assertThat(response.formatos()).containsExactly("ROUND_ROBIN");
    }

    @Test
    void crear_rechaza_minimo_mayor_al_maximo_sin_guardar() {
        var request = new PerfilCompetitivoRequest(1L, ModalidadCompetitiva.EQUIPOS,
                6, 5, List.of(10L), List.of(20L));

        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(IllegalArgumentException.class);
        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_rechaza_catalogo_inactivo() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").activo(true).build();
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(perfilRepository.findByJuegoId(1L)).thenReturn(Optional.empty());
        when(formatoRepository.findAllById(List.of(10L)))
                .thenReturn(List.of(new FormatoCompetitivo(10L, "ROUND_ROBIN", false)));

        assertThatThrownBy(() -> service.crear(requestEquipos())).isInstanceOf(BusinessException.class);
        verify(perfilRepository, never()).save(any());
    }

    @Test
    void crear_rechaza_si_falta_estadistica_obligatoria() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").activo(true).build();
        FormatoCompetitivo formato = new FormatoCompetitivo(10L, "ROUND_ROBIN", true);
        EstadisticaJuego seleccionada = new EstadisticaJuego(20L, "VICTORIAS", false, true);
        EstadisticaJuego obligatoria = new EstadisticaJuego(21L, "PARTIDAS_JUGADAS", true, true);
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(perfilRepository.findByJuegoId(1L)).thenReturn(Optional.empty());
        when(formatoRepository.findAllById(List.of(10L))).thenReturn(List.of(formato));
        when(estadisticaRepository.findAllById(List.of(20L))).thenReturn(List.of(seleccionada));
        when(estadisticaRepository.findByObligatoriaTrueAndActivaTrue()).thenReturn(List.of(obligatoria));

        assertThatThrownBy(() -> service.crear(requestEquipos()))
                .isInstanceOf(BusinessException.class).hasMessageContaining("PARTIDAS_JUGADAS");
        verify(perfilRepository, never()).save(any());
    }

    @Test
    void actualizar_bloquea_cambio_critico_con_torneo_activo_y_conserva_perfil() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").activo(true).build();
        PerfilCompetitivoJuego perfil = PerfilCompetitivoJuego.builder().id(30L).juego(juego)
                .modalidad(ModalidadCompetitiva.EQUIPOS).plantillaMinima(5).plantillaMaxima(5)
                .activo(true).formatosCompatibles(List.of(new FormatoCompetitivo(10L, "ROUND_ROBIN", true)))
                .estadisticas(List.of()).build();
        when(perfilRepository.findById(30L)).thenReturn(Optional.of(perfil));
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(perfilRepository.findByJuegoId(1L)).thenReturn(Optional.of(perfil));
        when(formatoRepository.findAllById(List.of(11L)))
                .thenReturn(List.of(new FormatoCompetitivo(11L, "SUIZO", true)));
        EstadisticaJuego estadistica = new EstadisticaJuego(20L, "VICTORIAS", true, true);
        when(estadisticaRepository.findAllById(List.of(20L))).thenReturn(List.of(estadistica));
        when(estadisticaRepository.findByObligatoriaTrueAndActivaTrue()).thenReturn(List.of(estadistica));
        when(torneoRepository.existsActivoByJuegoId(1L)).thenReturn(true);
        var cambio = new PerfilCompetitivoRequest(1L, ModalidadCompetitiva.EQUIPOS,
                5, 5, List.of(11L), List.of(20L));

        assertThatThrownBy(() -> service.actualizar(30L, cambio)).isInstanceOf(BusinessException.class);
        assertThat(perfil.getFormatosCompatibles()).extracting(FormatoCompetitivo::getId).containsExactly(10L);
        verify(perfilRepository, never()).save(any());
    }

    @Test
    void modalidad_individual_exige_plantilla_de_un_integrante() {
        var request = new PerfilCompetitivoRequest(1L, ModalidadCompetitiva.INDIVIDUAL,
                1, 2, List.of(10L), List.of(20L));
        assertThatThrownBy(() -> service.crear(request)).isInstanceOf(IllegalArgumentException.class);
    }

    private PerfilCompetitivoRequest requestEquipos() {
        return new PerfilCompetitivoRequest(1L, ModalidadCompetitiva.EQUIPOS,
                5, 5, List.of(10L), List.of(20L));
    }
}
