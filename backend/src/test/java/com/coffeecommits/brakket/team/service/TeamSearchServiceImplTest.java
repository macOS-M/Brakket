package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.common.dto.PageResponse;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamSearchServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private JuegoRepository juegoRepository;
    @InjectMocks
    private TeamSearchServiceImpl teamSearchService;

    private Equipo equipoDeEjemplo() {
        Juego juego = Juego.builder().id(1L).nombre("Valorant").genero("Shooter táctico").build();
        return Equipo.builder()
                .id(10L)
                .nombre("Los Invencibles")
                .descripcion("Equipo de prueba")
                .juego(juego)
                .estado("ACTIVO")
                .build();
    }

    @SuppressWarnings("unchecked")
    private void mockBusqueda(Page<Equipo> pagina) {
        when(equipoRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(pagina);
    }

    @Test
    void buscar_devuelve_la_pagina_mapeada_a_dto() {
        mockBusqueda(new PageImpl<>(List.of(equipoDeEjemplo()), PageRequest.of(0, 12), 1));

        PageResponse<EquipoBusquedaResponse> resultado =
                teamSearchService.buscar("  InVeNciBLes ", null, null, null, 0, 12);

        assertThat(resultado.items()).hasSize(1);
        assertThat(resultado.items().get(0).nombre()).isEqualTo("Los Invencibles");
        assertThat(resultado.items().get(0).juegoNombre()).isEqualTo("Valorant");
        assertThat(resultado.items().get(0).disciplina()).isEqualTo("Shooter táctico");
        assertThat(resultado.items().get(0).estado()).isEqualTo("ACTIVO");
        assertThat(resultado.totalElements()).isEqualTo(1);
        assertThat(resultado.totalPages()).isEqualTo(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_sin_filtros_lista_todo_paginado_y_ordenado_por_nombre() {
        mockBusqueda(new PageImpl<>(List.of(equipoDeEjemplo()), PageRequest.of(0, 12), 30));

        PageResponse<EquipoBusquedaResponse> resultado =
                teamSearchService.buscar(null, null, null, null, 0, 12);

        assertThat(resultado.totalElements()).isEqualTo(30);
        assertThat(resultado.totalPages()).isEqualTo(3);

        ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
        verify(equipoRepository).findAll(any(Specification.class), pageable.capture());
        assertThat(pageable.getValue().getPageNumber()).isZero();
        assertThat(pageable.getValue().getPageSize()).isEqualTo(12);
        assertThat(pageable.getValue().getSort())
                .isEqualTo(Sort.by(Sort.Direction.ASC, "nombre"));
    }

    @Test
    void buscar_devuelve_pagina_vacia_cuando_no_hay_coincidencias() {
        mockBusqueda(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PageResponse<EquipoBusquedaResponse> resultado =
                teamSearchService.buscar("nadie", null, null, null, 0, 12);

        assertThat(resultado.items()).isEmpty();
        assertThat(resultado.totalElements()).isZero();
    }

    @Test
    void buscar_acepta_estado_en_minusculas() {
        mockBusqueda(new PageImpl<>(List.of(), PageRequest.of(0, 12), 0));

        PageResponse<EquipoBusquedaResponse> resultado =
                teamSearchService.buscar(null, null, null, "disuelto", 0, 12);

        assertThat(resultado.items()).isEmpty();
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_falla_si_el_texto_supera_el_largo_maximo() {
        String textoLargo = "a".repeat(TeamSearchServiceImpl.LARGO_MAXIMO_TEXTO + 1);

        assertThatThrownBy(() -> teamSearchService.buscar(textoLargo, null, null, null, 0, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("largo máximo");
        verify(equipoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_falla_con_estado_invalido() {
        assertThatThrownBy(() -> teamSearchService.buscar(null, null, null, "CONGELADO", 0, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no es válido");
        verify(equipoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_falla_si_el_juego_del_filtro_no_existe() {
        when(juegoRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> teamSearchService.buscar(null, 99L, null, null, 0, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no existe");
        verify(equipoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_falla_si_la_disciplina_no_existe_en_el_catalogo() {
        when(juegoRepository.existsByGeneroIgnoreCase("Ajedrez")).thenReturn(false);

        assertThatThrownBy(() -> teamSearchService.buscar(null, null, "Ajedrez", null, 0, 12))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("disciplina");
        verify(equipoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }

    @Test
    @SuppressWarnings("unchecked")
    void buscar_falla_con_paginacion_invalida() {
        assertThatThrownBy(() -> teamSearchService.buscar(null, null, null, null, -1, 12))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> teamSearchService.buscar(null, null, null, null, 0, 0))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> teamSearchService.buscar(null, null, null, null, 0,
                TeamSearchServiceImpl.TAMANO_MAXIMO_PAGINA + 1))
                .isInstanceOf(IllegalArgumentException.class);
        verify(equipoRepository, never()).findAll(any(Specification.class), any(Pageable.class));
    }
}
