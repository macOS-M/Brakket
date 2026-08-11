package com.coffeecommits.brakket.statistics.service;

import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.league.repository.TemporadaRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EstadisticasHistoricasServiceTest {

    @Mock private NamedParameterJdbcTemplate jdbc;
    @Mock private UsuarioRepository usuarios;
    @Mock private EquipoRepository equipos;
    @Mock private JuegoRepository juegos;
    @Mock private TemporadaRepository temporadas;
    @Mock private LigaRepository ligas;
    @InjectMocks private EstadisticasHistoricasService service;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void estadisticas_oficiales_excluyen_reportadas_y_byes() {
        when(equipos.findById(20L)).thenReturn(Optional.of(
                Equipo.builder().id(20L).nombre("Nébula").build()));
        when(jdbc.query(anyString(), any(MapSqlParameterSource.class), any(RowMapper.class)))
                .thenReturn(List.of());
        when(jdbc.queryForObject(anyString(), any(MapSqlParameterSource.class),
                org.mockito.ArgumentMatchers.<Class<Integer>>any())).thenReturn(0);

        var respuesta = service.consultar(null, 20L, null, null, null, null, null);

        ArgumentCaptor<String> sqlOficial = ArgumentCaptor.forClass(String.class);
        verify(jdbc).query(sqlOficial.capture(), any(MapSqlParameterSource.class), any(RowMapper.class));
        assertThat(sqlOficial.getValue())
                .contains("p.estado='FINALIZADA'")
                .contains("p.equipo_a_id is not null")
                .contains("p.equipo_b_id is not null")
                .doesNotContain("p.estado in ('FINALIZADA','REPORTADA')");
        assertThat(respuesta.juegos()).isEmpty();
        assertThat(respuesta.resultadosNoDefinitivos()).isZero();
    }
}
