package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.admin.repository.LogAuditoriaRepository;
import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.team.dto.EditarEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRedSocialRepository;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.tournament.model.Inscripcion;
import com.coffeecommits.brakket.tournament.model.Torneo;
import com.coffeecommits.brakket.tournament.repository.InscripcionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Pruebas de editar() (RF-02): capitanía vigente vía miembro_equipo,
 * concurrencia optimista, contrato null/vacío y bloqueo por torneo.
 */
@ExtendWith(MockitoExtension.class)
class TeamRegistrationServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private JuegoRepository juegoRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private EquipoRedSocialRepository redSocialRepository;
    @Mock
    private InscripcionRepository inscripcionRepository;
    @Mock
    private LogAuditoriaRepository logAuditoriaRepository;
    @InjectMocks
    private TeamRegistrationServiceImpl service;

    private static final String CORREO = "capi@brakket.gg";

    private Usuario capitan() {
        return Usuario.builder().id(1L).nombre("Capi").correo(CORREO).build();
    }

    private Equipo equipo() {
        return Equipo.builder()
                .id(10L).nombre("Los Invencibles").logo("https://img.gg/logo.png")
                .descripcion("Equipo top").capitan(capitan())
                .juego(Juego.builder().id(3L).nombre("Valorant").genero("FPS").activo(true).build())
                .estado("ACTIVO").version(4L)
                .build();
    }

    private void actorEsCapitanActivo() {
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(capitan()));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .rol("CAPITAN").estado("ACTIVO").build()));
    }

    private EditarEquipoRequest request(String nombre, String logo, String descripcion,
                                        Long juegoId, Long version) {
        return new EditarEquipoRequest(nombre, logo, descripcion, juegoId, null, null, version);
    }

    @Test
    void editar_aplica_cambios_y_borra_campos_enviados_vacios() {
        Equipo equipo = equipo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        actorEsCapitanActivo();
        when(equipoRepository.findByNombreIgnoreCase("Nuevo Nombre")).thenReturn(Optional.empty());
        when(equipoRepository.save(equipo)).thenReturn(equipo);
        when(redSocialRepository.findByEquipoId(10L)).thenReturn(List.of());

        EquipoResponse resp = service.editar(10L, request("Nuevo Nombre", "", null, null, 4L), CORREO);

        assertThat(resp.nombre()).isEqualTo("Nuevo Nombre");
        assertThat(resp.logo()).isNull();                       // "" = borrar
        assertThat(resp.descripcion()).isEqualTo("Equipo top"); // null = no tocar
        verify(logAuditoriaRepository).save(any());
    }

    @Test
    void editar_rechaza_a_un_miembro_que_no_es_capitan_activo() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo()));
        when(usuarioRepository.findByCorreo(CORREO)).thenReturn(Optional.of(capitan()));
        // El fundador sigue en equipo.capitan_id, pero RF-09 lo degradó a suplente.
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 1L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .rol("SUPLENTE").estado("ACTIVO").build()));

        assertThatThrownBy(() -> service.editar(10L, request("Otro", null, null, null, null), CORREO))
                .isInstanceOf(AccessDeniedException.class);
        verify(equipoRepository, never()).save(any());
    }

    @Test
    void editar_rechaza_version_desactualizada() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo())); // versión actual: 4
        actorEsCapitanActivo();

        assertThatThrownBy(() -> service.editar(10L, request("Otro", null, null, null, 3L), CORREO))
                .isInstanceOf(ObjectOptimisticLockingFailureException.class);
        verify(equipoRepository, never()).save(any());
    }

    @Test
    void editar_rechaza_nombre_duplicado_ignorando_mayusculas() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo()));
        actorEsCapitanActivo();
        Equipo otro = Equipo.builder().id(99L).nombre("fnatic").capitan(capitan()).build();
        when(equipoRepository.findByNombreIgnoreCase("FNATIC")).thenReturn(Optional.of(otro));

        assertThatThrownBy(() -> service.editar(10L, request("FNATIC", null, null, null, null), CORREO))
                .isInstanceOf(BusinessException.class);
        verify(equipoRepository, never()).save(any());
    }

    @Test
    void editar_rechaza_equipo_disuelto() {
        Equipo disuelto = equipo();
        disuelto.setEstado("DISUELTO");
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(disuelto));
        actorEsCapitanActivo();

        assertThatThrownBy(() -> service.editar(10L, request("Otro", null, null, null, null), CORREO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disuelto");
    }

    @Test
    void editar_bloquea_cambio_de_disciplina_con_torneo_futuro() {
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo()));
        actorEsCapitanActivo();
        Juego fifa = Juego.builder().id(7L).nombre("FIFA").genero("Deportes").activo(true).build();
        when(juegoRepository.findById(7L)).thenReturn(Optional.of(fifa));
        Torneo futuro = Torneo.builder()
                .fechaInicio(LocalDate.now().plusDays(1).atStartOfDay())
                .fechaFin(LocalDate.now().plusDays(10).atStartOfDay())
                .build();
        when(inscripcionRepository.findByEquipoId(10L)).thenReturn(List.of(
                Inscripcion.builder().torneo(futuro).estado("PENDIENTE").build()));

        assertThatThrownBy(() -> service.editar(10L, request(null, null, null, 7L, null), CORREO))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("torneo");
    }

    @Test
    void editar_ignora_inscripciones_cerradas_al_cambiar_disciplina() {
        Equipo equipo = equipo();
        when(equipoRepository.findById(10L)).thenReturn(Optional.of(equipo));
        actorEsCapitanActivo();
        Juego fifa = Juego.builder().id(7L).nombre("FIFA").genero("Deportes").activo(true).build();
        when(juegoRepository.findById(7L)).thenReturn(Optional.of(fifa));
        Torneo futuro = Torneo.builder()
                .fechaInicio(LocalDate.now().plusDays(1).atStartOfDay())
                .fechaFin(LocalDate.now().plusDays(10).atStartOfDay())
                .build();
        when(inscripcionRepository.findByEquipoId(10L)).thenReturn(List.of(
                Inscripcion.builder().torneo(futuro).estado("CANCELADA").build()));
        when(equipoRepository.save(equipo)).thenReturn(equipo);
        when(redSocialRepository.findByEquipoId(10L)).thenReturn(List.of());

        EquipoResponse resp = service.editar(10L, request(null, null, null, 7L, null), CORREO);

        assertThat(resp.juegoId()).isEqualTo(7L);
    }
}
