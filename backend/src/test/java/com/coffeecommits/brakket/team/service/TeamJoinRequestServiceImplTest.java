package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.notification.service.NotificationService;
import com.coffeecommits.brakket.team.dto.SolicitarUnionRequest;
import com.coffeecommits.brakket.team.dto.SolicitudUnionResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.model.SolicitudUnion;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.team.repository.SolicitudUnionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.argThat;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeamJoinRequestServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private com.coffeecommits.brakket.auth.repository.UsuarioRolRepository usuarioRolRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private SolicitudUnionRepository solicitudRepository;
    @Mock
    private NotificationService notificationService;

    private TeamJoinRequestServiceImpl service;

    private final Usuario jugador = Usuario.builder().id(5L).nombre("Ana").correo("ana@x.com").build();
    private final Usuario capitan = Usuario.builder().id(1L).nombre("Capi").correo("capi@x.com").build();
    private final Equipo equipo = Equipo.builder().id(9L).nombre("Nébula").estado("ACTIVO").build();

    @BeforeEach
    void setUp() {
        service = new TeamJoinRequestServiceImpl(equipoRepository, usuarioRepository,
                usuarioRolRepository, miembroEquipoRepository, solicitudRepository, notificationService);
        lenient().when(usuarioRepository.findByCorreo("ana@x.com")).thenReturn(Optional.of(jugador));
        lenient().when(usuarioRepository.findByCorreo("capi@x.com")).thenReturn(Optional.of(capitan));
        lenient().when(solicitudRepository.save(any(SolicitudUnion.class)))
                .thenAnswer(inv -> {
                    SolicitudUnion s = inv.getArgument(0);
                    if (s.getId() == null) {
                        s.setId(30L);
                    }
                    return s;
                });
    }

    private MiembroEquipo miembroCapitan() {
        return MiembroEquipo.builder().equipo(equipo).usuario(capitan)
                .rol("CAPITAN").estado("ACTIVO").build();
    }

    @Test
    void solicitar_crea_pendiente_y_notifica_al_capitan() {
        when(equipoRepository.findById(9L)).thenReturn(Optional.of(equipo));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.empty());
        when(solicitudRepository.existsByEquipoIdAndJugadorIdAndEstado(9L, 5L, "PENDIENTE"))
                .thenReturn(false);
        when(miembroEquipoRepository.findByEquipoId(9L)).thenReturn(List.of(miembroCapitan()));

        SolicitudUnionResponse resp = service.solicitar(9L, "ana@x.com",
                new SolicitarUnionRequest("  Quiero competir  "));

        assertThat(resp.estado()).isEqualTo("PENDIENTE");
        assertThat(resp.mensaje()).isEqualTo("Quiero competir");
        verify(notificationService).notificar(eq(capitan), eq("SOLICITUD_UNION"),
                any(), eq("SolicitudUnion"), eq(30L));
    }

    @Test
    void solicitar_rechaza_miembros_activos_y_duplicados() {
        when(equipoRepository.findById(9L)).thenReturn(Optional.of(equipo));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.of(
                MiembroEquipo.builder().equipo(equipo).usuario(jugador)
                        .rol("TITULAR").estado("ACTIVO").build()));

        assertThatThrownBy(() -> service.solicitar(9L, "ana@x.com", new SolicitarUnionRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya sos parte");

        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.empty());
        when(solicitudRepository.existsByEquipoIdAndJugadorIdAndEstado(9L, 5L, "PENDIENTE"))
                .thenReturn(true);
        assertThatThrownBy(() -> service.solicitar(9L, "ana@x.com", new SolicitarUnionRequest(null)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pendiente");
    }

    @Test
    void responder_acepta_y_da_de_alta_como_titular() {
        SolicitudUnion solicitud = SolicitudUnion.builder().id(30L).equipo(equipo).jugador(jugador)
                .estado("PENDIENTE").fechaCreacion(LocalDateTime.now()).build();
        when(solicitudRepository.findById(30L)).thenReturn(Optional.of(solicitud));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 1L))
                .thenReturn(Optional.of(miembroCapitan()));
        when(miembroEquipoRepository.findByEquipoId(9L)).thenReturn(List.of(miembroCapitan()));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.empty());

        SolicitudUnionResponse resp = service.responder(30L, "capi@x.com", true);

        assertThat(resp.estado()).isEqualTo("ACEPTADA");
        verify(miembroEquipoRepository).save(argThat(m ->
                "TITULAR".equals(m.getRol()) && "ACTIVO".equals(m.getEstado())
                        && m.getUsuario().getId().equals(5L)));
        verify(notificationService).notificar(eq(jugador), eq("SOLICITUD_ACEPTADA"),
                any(), any(), anyLong());
    }

    @Test
    void responder_exige_ser_capitan_del_equipo() {
        SolicitudUnion solicitud = SolicitudUnion.builder().id(30L).equipo(equipo).jugador(jugador)
                .estado("PENDIENTE").fechaCreacion(LocalDateTime.now()).build();
        when(solicitudRepository.findById(30L)).thenReturn(Optional.of(solicitud));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.responder(30L, "ana@x.com", true))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void mis_equipos_solo_incluye_membresias_activas() {
        Equipo otro = Equipo.builder().id(11L).nombre("Viejo").estado("ACTIVO").build();
        when(miembroEquipoRepository.findByUsuarioId(5L)).thenReturn(List.of(
                MiembroEquipo.builder().equipo(equipo).usuario(jugador).rol("TITULAR").estado("ACTIVO").build(),
                MiembroEquipo.builder().equipo(otro).usuario(jugador).rol("TITULAR").estado("EXPULSADO").build()));

        assertThat(service.misEquipos("ana@x.com"))
                .extracting(e -> e.id())
                .containsExactly(9L);
    }
}
