package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.auth.repository.UsuarioRolRepository;
import com.coffeecommits.brakket.notification.service.NotificationService;
import com.coffeecommits.brakket.team.dto.ResponderInvitacionRequest;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.InvitacionEquipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.InvitacionEquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Respuesta a una invitación de equipo (RF-11).
 *
 * <p>Lo que se protege acá es la <b>reincorporación</b>: un jugador expulsado
 * deja su fila de membresía en la tabla, y aceptar una invitación nueva
 * insertaba otra que chocaba con la restricción única de (equipo, usuario). El
 * error se traducía a "El jugador ya pertenece al equipo", que además de
 * bloquear era falso.</p>
 */
@ExtendWith(MockitoExtension.class)
class TeamInvitationServiceImplTest {

    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private UsuarioRolRepository usuarioRolRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private InvitacionEquipoRepository invitacionRepository;
    @Mock
    private NotificationService notificationService;

    private TeamInvitationServiceImpl service;

    private final Usuario jugador = Usuario.builder().id(5L).nombre("Pedro").correo("pedro@x.com").build();
    private final Usuario capitan = Usuario.builder().id(1L).nombre("Bruno").correo("bruno@x.com").build();
    private final Equipo equipo = Equipo.builder().id(9L).nombre("Lobos Demo").estado("ACTIVO").build();

    @BeforeEach
    void setUp() {
        service = new TeamInvitationServiceImpl(equipoRepository, usuarioRepository,
                usuarioRolRepository, miembroEquipoRepository, invitacionRepository, notificationService);
    }

    private InvitacionEquipo invitacionPendiente() {
        return InvitacionEquipo.builder()
                .id(30L).equipo(equipo).jugador(jugador).creadoPor(capitan)
                .rolPropuesto("TITULAR").estado("PENDIENTE").build();
    }

    @Test
    void aceptar_reactiva_la_membresia_de_un_jugador_expulsado() {
        MiembroEquipo expulsado = MiembroEquipo.builder()
                .id(77L).equipo(equipo).usuario(jugador)
                .estado("EXPULSADO").rol("TITULAR")
                .fechaUnion(LocalDate.now().minusMonths(2))
                .build();

        when(usuarioRepository.findByCorreo("pedro@x.com")).thenReturn(Optional.of(jugador));
        when(invitacionRepository.findByIdAndJugadorId(30L, 5L)).thenReturn(Optional.of(invitacionPendiente()));
        when(miembroEquipoRepository.findByEquipoId(9L)).thenReturn(List.of(expulsado));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.of(expulsado));

        service.responder(30L, new ResponderInvitacionRequest(true), "pedro@x.com");

        ArgumentCaptor<MiembroEquipo> guardado = ArgumentCaptor.forClass(MiembroEquipo.class);
        verify(miembroEquipoRepository).save(guardado.capture());

        // La misma fila, reactivada: insertar una nueva chocaba con la unica de
        // (equipo, usuario) y el jugador quedaba sin poder volver al equipo.
        assertThat(guardado.getValue().getId()).isEqualTo(77L);
        assertThat(guardado.getValue().getEstado()).isEqualTo("ACTIVO");
        assertThat(guardado.getValue().getFechaUnion()).isEqualTo(LocalDate.now());
    }

    @Test
    void aceptar_crea_la_membresia_cuando_el_jugador_es_nuevo() {
        when(usuarioRepository.findByCorreo("pedro@x.com")).thenReturn(Optional.of(jugador));
        when(invitacionRepository.findByIdAndJugadorId(30L, 5L)).thenReturn(Optional.of(invitacionPendiente()));
        when(miembroEquipoRepository.findByEquipoId(9L)).thenReturn(List.of());
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(9L, 5L)).thenReturn(Optional.empty());

        service.responder(30L, new ResponderInvitacionRequest(true), "pedro@x.com");

        ArgumentCaptor<MiembroEquipo> guardado = ArgumentCaptor.forClass(MiembroEquipo.class);
        verify(miembroEquipoRepository).save(guardado.capture());

        assertThat(guardado.getValue().getId()).isNull();
        assertThat(guardado.getValue().getEstado()).isEqualTo("ACTIVO");
        assertThat(guardado.getValue().getRol()).isEqualTo("TITULAR");
    }

    @Test
    void rechazar_no_toca_la_membresia() {
        when(usuarioRepository.findByCorreo("pedro@x.com")).thenReturn(Optional.of(jugador));
        when(invitacionRepository.findByIdAndJugadorId(30L, 5L)).thenReturn(Optional.of(invitacionPendiente()));

        service.responder(30L, new ResponderInvitacionRequest(false), "pedro@x.com");

        verify(miembroEquipoRepository, org.mockito.Mockito.never()).save(any());
    }
}
