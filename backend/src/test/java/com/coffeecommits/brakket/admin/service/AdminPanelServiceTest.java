package com.coffeecommits.brakket.admin.service;

import com.coffeecommits.brakket.admin.dto.LogAuditoriaResponse;
import com.coffeecommits.brakket.admin.dto.PanelGlobalResponse;
import com.coffeecommits.brakket.admin.dto.ResumenPlataformaResponse;
import com.coffeecommits.brakket.admin.model.LogAuditoria;
import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.dispute.repository.DisputaRepository;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.tournament.model.EstadoTorneo;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminPanelServiceTest {

    @Mock private UsuarioRepository usuarioRepository;
    @Mock private EquipoRepository equipoRepository;
    @Mock private JuegoRepository juegoRepository;
    @Mock private LigaRepository ligaRepository;
    @Mock private TorneoRepository torneoRepository;
    @Mock private TransmisionTwitchRepository transmisionRepository;
    @Mock private DisputaRepository disputaRepository;
    @Mock private LogAuditoriaRepository logAuditoriaRepository;

    @InjectMocks
    private AdminPanelService service;

    @Test
    void resumen_agrega_los_conteos_de_todos_los_modulos() {
        when(usuarioRepository.count()).thenReturn(50L);
        when(usuarioRepository.countByBloqueadoTrue()).thenReturn(3L);
        when(equipoRepository.count()).thenReturn(12L);
        when(juegoRepository.count()).thenReturn(8L);
        when(juegoRepository.countByActivoTrue()).thenReturn(6L);
        when(ligaRepository.count()).thenReturn(4L);
        when(torneoRepository.count()).thenReturn(20L);
        when(torneoRepository.countByEstado(EstadoTorneo.EN_CURSO)).thenReturn(5L);
        when(transmisionRepository.countByActivaTrue()).thenReturn(2L);
        when(disputaRepository.count()).thenReturn(7L);

        ResumenPlataformaResponse r = service.resumen();

        assertThat(r.usuarios()).isEqualTo(50L);
        assertThat(r.usuariosBloqueados()).isEqualTo(3L);
        assertThat(r.equipos()).isEqualTo(12L);
        assertThat(r.juegos()).isEqualTo(8L);
        assertThat(r.juegosActivos()).isEqualTo(6L);
        assertThat(r.ligas()).isEqualTo(4L);
        assertThat(r.torneos()).isEqualTo(20L);
        assertThat(r.torneosEnCurso()).isEqualTo(5L);
        assertThat(r.transmisionesActivas()).isEqualTo(2L);
        assertThat(r.disputas()).isEqualTo(7L);
    }

    @Test
    void panel_incluye_resumen_y_actividad_reciente_mapeada() {
        stubConteosEnCero();
        Usuario actor = Usuario.builder().id(1L).nombre("Ana").correo("ana@brakket.gg").build();
        LogAuditoria log = LogAuditoria.builder()
                .id(99L).usuario(actor).accion("ROL_ASIGNADO: ARBITRO")
                .entidad("usuario").entidadId(42L).fecha(LocalDateTime.now()).build();
        when(logAuditoriaRepository.findAllByOrderByFechaDescIdDesc(any())).thenReturn(List.of(log));

        PanelGlobalResponse panel = service.panel();

        assertThat(panel.resumen()).isNotNull();
        assertThat(panel.actividadReciente()).hasSize(1);
        LogAuditoriaResponse entrada = panel.actividadReciente().get(0);
        assertThat(entrada.accion()).isEqualTo("ROL_ASIGNADO: ARBITRO");
        assertThat(entrada.actorNombre()).isEqualTo("Ana");
        assertThat(entrada.entidadId()).isEqualTo(42L);
    }

    @Test
    void auditoria_reciente_mapea_actor_nulo_sin_romper() {
        LogAuditoria sinActor = LogAuditoria.builder()
                .id(5L).usuario(null).accion("SISTEMA").entidad("juego")
                .entidadId(1L).fecha(LocalDateTime.now()).build();
        when(logAuditoriaRepository.findAllByOrderByFechaDescIdDesc(any()))
                .thenReturn(List.of(sinActor));

        List<LogAuditoriaResponse> lista = service.auditoriaReciente(10);

        assertThat(lista).hasSize(1);
        assertThat(lista.get(0).actorNombre()).isNull();
        assertThat(lista.get(0).actorCorreo()).isNull();
    }

    private void stubConteosEnCero() {
        when(usuarioRepository.count()).thenReturn(0L);
        when(usuarioRepository.countByBloqueadoTrue()).thenReturn(0L);
        when(equipoRepository.count()).thenReturn(0L);
        when(juegoRepository.count()).thenReturn(0L);
        when(juegoRepository.countByActivoTrue()).thenReturn(0L);
        when(ligaRepository.count()).thenReturn(0L);
        when(torneoRepository.count()).thenReturn(0L);
        when(torneoRepository.countByEstado(EstadoTorneo.EN_CURSO)).thenReturn(0L);
        when(transmisionRepository.countByActivaTrue()).thenReturn(0L);
        when(disputaRepository.count()).thenReturn(0L);
    }
}
