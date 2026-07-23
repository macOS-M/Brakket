package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.dto.JuegoExternoResponse;
import com.coffeecommits.brakket.game.dto.JuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoResponse;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.repository.LigaRepository;
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
class GameServiceImplTest {

    @Mock
    private JuegoRepository juegoRepository;
    @Mock
    private LigaRepository ligaRepository;
    @Mock
    private ExternalGameSearchService externalGameSearchService;
    @InjectMocks
    private GameServiceImpl gameService;

    @Test
    void crear_guarda_el_juego_como_activo() {
        when(juegoRepository.findByNombre("Valorant")).thenReturn(Optional.empty());
        when(juegoRepository.save(any(Juego.class))).thenAnswer(inv -> {
            Juego j = inv.getArgument(0);
            j.setId(1L);
            return j;
        });

        JuegoResponse resp = gameService.crear(new JuegoRequest(
                "Valorant", "Shooter táctico", "Shooter 5v5 de Riot", "https://media.rawg.io/valorant.jpg"));

        assertThat(resp.id()).isEqualTo(1L);
        assertThat(resp.nombre()).isEqualTo("Valorant");
        assertThat(resp.imagenUrl()).isEqualTo("https://media.rawg.io/valorant.jpg");
        assertThat(resp.activo()).isTrue();
    }

    @Test
    void crear_falla_con_409_si_el_nombre_ya_existe() {
        when(juegoRepository.findByNombre("Valorant"))
                .thenReturn(Optional.of(Juego.builder().id(9L).nombre("Valorant").build()));

        assertThatThrownBy(() -> gameService.crear(new JuegoRequest("Valorant", "Shooter", null, null)))
                .isInstanceOf(BusinessException.class);
        verify(juegoRepository, never()).save(any(Juego.class));
    }

    @Test
    void editar_falla_si_el_nuevo_nombre_lo_usa_otro_juego() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").genero("Shooter").activo(true).build();
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(juegoRepository.findByNombre("Counter-Strike 2"))
                .thenReturn(Optional.of(Juego.builder().id(2L).nombre("Counter-Strike 2").build()));

        assertThatThrownBy(() -> gameService.editar(1L, new JuegoRequest("Counter-Strike 2", "Shooter", null, null)))
                .isInstanceOf(BusinessException.class);
        verify(juegoRepository, never()).save(any(Juego.class));
    }

    @Test
    void editar_permite_conservar_el_propio_nombre() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").genero("Shooter").activo(true).build();
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(juegoRepository.findByNombre("Valorant")).thenReturn(Optional.of(juego));
        when(juegoRepository.save(juego)).thenReturn(juego);

        JuegoResponse resp = gameService.editar(
                1L, new JuegoRequest("Valorant", "FPS táctico", "Nueva descripción", null));

        assertThat(resp.genero()).isEqualTo("FPS táctico");
        assertThat(resp.descripcion()).isEqualTo("Nueva descripción");
    }

    @Test
    void importar_devuelve_el_existente_sin_consultar_la_api() {
        Juego activo = Juego.builder().id(3L).nombre("Valorant").genero("Shooter").activo(true).build();
        when(juegoRepository.findByNombreIgnoreCase("valorant")).thenReturn(Optional.of(activo));

        JuegoResponse resp = gameService.importarDesdeExterno("valorant", false);

        assertThat(resp.id()).isEqualTo(3L);
        verify(externalGameSearchService, never()).buscar(any());
    }

    @Test
    void importar_no_resucita_un_juego_desactivado_por_un_admin() {
        Juego inactivo = Juego.builder().id(3L).nombre("Valorant").genero("Shooter").activo(false).build();
        when(juegoRepository.findByNombreIgnoreCase("valorant")).thenReturn(Optional.of(inactivo));

        assertThatThrownBy(() -> gameService.importarDesdeExterno("valorant", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("desactivado por un administrador");
        verify(externalGameSearchService, never()).buscar(any());
        verify(juegoRepository, never()).save(any(Juego.class));
    }

    @Test
    void importar_crea_el_juego_con_los_datos_del_catalogo_externo() {
        when(juegoRepository.findByNombreIgnoreCase("apex")).thenReturn(Optional.empty());
        when(externalGameSearchService.buscar("apex")).thenReturn(List.of(
                new JuegoExternoResponse("apex-legends", "Apex Legends", "Shooter", "https://media.rawg.io/apex.jpg")));
        when(juegoRepository.findByNombreIgnoreCase("Apex Legends")).thenReturn(Optional.empty());
        when(juegoRepository.save(any(Juego.class))).thenAnswer(inv -> {
            Juego j = inv.getArgument(0);
            j.setId(9L);
            return j;
        });

        JuegoResponse resp = gameService.importarDesdeExterno("apex", false);

        assertThat(resp.nombre()).isEqualTo("Apex Legends");
        assertThat(resp.genero()).isEqualTo("Shooter");
        assertThat(resp.imagenUrl()).isEqualTo("https://media.rawg.io/apex.jpg");
        assertThat(resp.activo()).isTrue();
    }

    @Test
    void importar_falla_claro_si_el_catalogo_externo_no_lo_encuentra() {
        when(juegoRepository.findByNombreIgnoreCase("juego-inventado")).thenReturn(Optional.empty());
        when(externalGameSearchService.buscar("juego-inventado")).thenReturn(List.of());

        assertThatThrownBy(() -> gameService.importarDesdeExterno("juego-inventado", false))
                .isInstanceOf(BusinessException.class);
        verify(juegoRepository, never()).save(any(Juego.class));
    }

    @Test
    void desactivar_falla_si_el_juego_tiene_ligas_asociadas() {
        when(juegoRepository.findById(1L))
                .thenReturn(Optional.of(Juego.builder().id(1L).nombre("Valorant").activo(true).build()));
        when(ligaRepository.existsByJuegoId(1L)).thenReturn(true);

        assertThatThrownBy(() -> gameService.desactivar(1L))
                .isInstanceOf(BusinessException.class);
        verify(juegoRepository, never()).save(any(Juego.class));
    }

    @Test
    void desactivar_hace_baja_logica_sin_borrar() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").activo(true).build();
        when(juegoRepository.findById(1L)).thenReturn(Optional.of(juego));
        when(ligaRepository.existsByJuegoId(1L)).thenReturn(false);
        when(juegoRepository.save(juego)).thenReturn(juego);

        gameService.desactivar(1L);

        assertThat(juego.getActivo()).isFalse();
        verify(juegoRepository).save(juego);
        verify(juegoRepository, never()).delete(any(Juego.class));
    }

    @Test
    void obtenerPorId_lanza_404_si_no_existe() {
        when(juegoRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> gameService.obtenerPorId(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void listarActivos_mapea_los_juegos_a_dto() {
        when(juegoRepository.findByActivoTrue()).thenReturn(List.of(
                Juego.builder().id(1L).nombre("Valorant").genero("Shooter").activo(true).build(),
                Juego.builder().id(2L).nombre("Rocket League").genero("Deportes").activo(true).build()));

        List<JuegoResponse> resp = gameService.listarActivos();

        assertThat(resp).hasSize(2);
        assertThat(resp).extracting(JuegoResponse::nombre)
                .containsExactly("Valorant", "Rocket League");
    }
}
