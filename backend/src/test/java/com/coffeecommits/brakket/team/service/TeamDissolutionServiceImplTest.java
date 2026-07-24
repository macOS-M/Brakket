package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.transfer.repository.HistorialTransferenciaRepository;
import com.coffeecommits.brakket.transfer.repository.SolicitudTransferenciaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamDissolutionServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private PartidaRepository partidaRepository;
    @Mock
    private TorneoRepository torneoRepository;
    @Mock
    private HistorialTransferenciaRepository historialTransferenciaRepository;
    @Mock
    private SolicitudTransferenciaRepository solicitudTransferenciaRepository;
    @Mock
    private LogAuditoriaRepository logAuditoriaRepository;
    @InjectMocks
    private TeamDissolutionServiceImpl service;

    private final Usuario capitan = Usuario.builder().id(1L).correo("cap@x.com").nombre("Capi").build();
    private final Usuario jugador = Usuario.builder().id(2L).correo("jug@x.com").nombre("Juga").build();
    private final Usuario admin = Usuario.builder().id(3L).correo("adm@x.com").nombre("Admin").build();

    private Equipo equipoActivo() {
        return Equipo.builder().id(10L).nombre("Coffee&Commits").capitan(capitan).estado("ACTIVO").build();
    }

    private Equipo equipoDisuelto() {
        Equipo equipo = equipoActivo();
        equipo.setEstado("DISUELTO");
        return equipo;
    }

    private MiembroEquipo miembro(Usuario usuario, String rol, String estado) {
        return MiembroEquipo.builder()
                .id(usuario.getId() + 100).usuario(usuario).estado(estado).rol(rol).build();
    }

    private void solicitanteEsCapitanActivo() {
        when(usuarioRepository.findByCorreo("cap@x.com")).thenReturn(Optional.of(capitan));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(miembro(capitan, "CAPITAN", "ACTIVO")));
    }

    /** El equipo no dejó rastro competitivo: se puede borrar físicamente. */
    private void sinHistorialCompetitivo() {
        lenient().when(inscripcionRepository.existsByEquipoId(10L)).thenReturn(false);
        lenient().when(partidaRepository.existsByEquipoAId(10L)).thenReturn(false);
        lenient().when(partidaRepository.existsByEquipoBId(10L)).thenReturn(false);
        lenient().when(partidaRepository.existsByGanadorId(10L)).thenReturn(false);
        lenient().when(torneoRepository.existsByCampeonId(10L)).thenReturn(false);
        lenient().when(historialTransferenciaRepository
                .existsByEquipoOrigenIdOrEquipoDestinoId(10L, 10L)).thenReturn(false);
        lenient().when(solicitudTransferenciaRepository
                .existsByEquipoOrigenIdOrEquipoDestinoId(10L, 10L)).thenReturn(false);
    }

    @Test
    void disolver_marca_el_equipo_como_disuelto_con_fecha_responsable_y_motivo() {
        Equipo equipo = equipoActivo();
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(false);
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        EquipoResponse resp = service.disolver(10L,
                new DisolverEquipoRequest(true, "  Nos retiramos de la liga  "), "cap@x.com", false);

        assertThat(equipo.getEstado()).isEqualTo("DISUELTO");
        assertThat(equipo.getFechaDisolucion()).isNotNull();
        assertThat(equipo.getDisueltoPor()).isEqualTo(capitan);
        assertThat(equipo.getMotivoDisolucion()).isEqualTo("Nos retiramos de la liga");
        assertThat(resp.estado()).isEqualTo("DISUELTO");
        verify(equipoRepository).save(equipo);
    }

    @Test
    void disolver_acepta_motivo_vacio_y_lo_guarda_como_null() {
        Equipo equipo = equipoActivo();
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(false);
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        service.disolver(10L, new DisolverEquipoRequest(true, "   "), "cap@x.com", false);

        assertThat(equipo.getMotivoDisolucion()).isNull();
    }

    @Test
    void disolver_falla_si_el_solicitante_no_es_capitan() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        when(usuarioRepository.findByCorreo("jug@x.com")).thenReturn(Optional.of(jugador));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L))
                .thenReturn(Optional.of(miembro(jugador, "TITULAR", "ACTIVO")));

        assertThatThrownBy(() -> service.disolver(10L,
                new DisolverEquipoRequest(true, null), "jug@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitán");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_falla_si_el_solicitante_no_pertenece_al_equipo() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        when(usuarioRepository.findByCorreo("jug@x.com")).thenReturn(Optional.of(jugador));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disolver(10L,
                new DisolverEquipoRequest(true, null), "jug@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitán");
    }

    @Test
    void un_admin_disuelve_cualquier_equipo_sin_ser_miembro() {
        Equipo equipo = equipoActivo();
        when(usuarioRepository.findByCorreo("adm@x.com")).thenReturn(Optional.of(admin));
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(false);
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        service.disolver(10L, new DisolverEquipoRequest(true, "moderación"), "adm@x.com", true);

        assertThat(equipo.getEstado()).isEqualTo("DISUELTO");
        verify(miembroEquipoRepository, never()).findByEquipoIdAndUsuarioId(anyLong(), anyLong());
    }

    @Test
    void disolver_falla_si_el_equipo_ya_esta_disuelto() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoDisuelto()));

        assertThatThrownBy(() -> service.disolver(10L,
                new DisolverEquipoRequest(true, null), "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está disuelto");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_bloquea_si_hay_inscripciones_activas_en_torneos() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.disolver(10L,
                new DisolverEquipoRequest(true, null), "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("inscripciones activas");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_bloquea_si_hay_partidas_pendientes() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        when(inscripcionRepository.existsInscripcionActivaPorEquipo(10L)).thenReturn(false);
        when(partidaRepository.existsPartidaPendientePorEquipo(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.disolver(10L,
                new DisolverEquipoRequest(true, null), "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("partidas pendientes");
        verify(equipoRepository, never()).save(any(Equipo.class));
    }

    @Test
    void disolver_lanza_404_si_el_equipo_no_existe() {
        // La existencia se valida antes que la capitanía: 404 directo.
        when(equipoRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.disolver(10L,
                new DisolverEquipoRequest(true, null), "cap@x.com", false))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------- reactivar ----------

    @Test
    void reactivar_devuelve_el_equipo_a_activo_y_limpia_la_disolucion() {
        Equipo equipo = equipoDisuelto();
        equipo.setMotivoDisolucion("nos retiramos");
        equipo.setDisueltoPor(capitan);
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        when(equipoRepository.save(equipo)).thenReturn(equipo);

        EquipoResponse resp = service.reactivar(10L, "cap@x.com", false);

        assertThat(resp.estado()).isEqualTo("ACTIVO");
        assertThat(equipo.getFechaDisolucion()).isNull();
        assertThat(equipo.getMotivoDisolucion()).isNull();
        assertThat(equipo.getDisueltoPor()).isNull();
    }

    @Test
    void reactivar_falla_si_el_equipo_esta_activo_o_bloqueado() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));
        assertThatThrownBy(() -> service.reactivar(10L, "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya está activo");

        Equipo bloqueado = equipoActivo();
        bloqueado.setEstado("BLOQUEADO");
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(bloqueado));
        assertThatThrownBy(() -> service.reactivar(10L, "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloqueado");
    }

    // ---------- eliminar ----------

    @Test
    void el_capitan_elimina_su_equipo_disuelto_sin_historial() {
        solicitanteEsCapitanActivo();
        Equipo equipo = equipoDisuelto();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        sinHistorialCompetitivo();

        service.eliminar(10L, "cap@x.com", false);

        verify(equipoRepository).delete(equipo);
    }

    @Test
    void el_capitan_no_elimina_un_equipo_sin_disolverlo_antes() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoActivo()));

        assertThatThrownBy(() -> service.eliminar(10L, "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Disolvé");
        verify(equipoRepository, never()).delete(any(Equipo.class));
    }

    @Test
    void un_admin_elimina_un_equipo_activo_aunque_figure_como_miembro() {
        Equipo equipo = equipoActivo();
        when(usuarioRepository.findByCorreo("adm@x.com")).thenReturn(Optional.of(admin));
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        sinHistorialCompetitivo();

        service.eliminar(10L, "adm@x.com", true);

        verify(equipoRepository).delete(equipo);
    }

    @Test
    void eliminar_se_bloquea_si_el_equipo_tiene_historial_competitivo() {
        solicitanteEsCapitanActivo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipoDisuelto()));
        sinHistorialCompetitivo();
        when(inscripcionRepository.existsByEquipoId(10L)).thenReturn(true);

        assertThatThrownBy(() -> service.eliminar(10L, "cap@x.com", false))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("historial competitivo");
        verify(equipoRepository, never()).delete(any(Equipo.class));
    }
}
