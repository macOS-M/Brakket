package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.notification.service.NotificationService;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.transfer.dto.ResponderTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;
import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;
import com.coffeecommits.brakket.transfer.repository.SolicitudTransferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferResponseServiceImplTest {

    private static final ResponderTransferenciaRequest ACEPTAR =
            new ResponderTransferenciaRequest("ACEPTAR");
    private static final ResponderTransferenciaRequest RECHAZAR =
            new ResponderTransferenciaRequest("RECHAZAR");

    @Mock
    private SolicitudTransferenciaRepository transferenciaRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificationService notificationService;
    @Mock
    private HistorialTransferenciaService historialTransferenciaService; // RF-14
    @InjectMocks
    private TransferResponseServiceImpl service;

    private Usuario capitanDestino;
    private Usuario capitanOrigen;
    private Usuario jugador;
    private Equipo origen;
    private Equipo destino;
    private SolicitudTransferencia solicitud;

    @BeforeEach
    void setUp() {
        capitanDestino = Usuario.builder().id(1L).nombre("Capi Destino")
                .correo("capi.destino@brakket.local").build();
        capitanOrigen = Usuario.builder().id(2L).nombre("Capi Origen")
                .correo("capi.origen@brakket.local").build();
        jugador = Usuario.builder().id(3L).nombre("Jugador Estrella")
                .correo("jugador@brakket.local").build();
        origen = Equipo.builder().id(10L).nombre("Origen FC").capitan(capitanOrigen)
                .estado("ACTIVO").build();
        destino = Equipo.builder().id(20L).nombre("Destino FC").capitan(capitanDestino)
                .estado("ACTIVO").build();
        solicitud = SolicitudTransferencia.builder()
                .id(100L).jugador(jugador).equipoOrigen(origen).equipoDestino(destino)
                .solicitante(capitanDestino).rolPropuesto("TITULAR")
                .build();

        lenient().when(usuarioRepository.findByCorreo("jugador@brakket.local"))
                .thenReturn(Optional.of(jugador));
        lenient().when(usuarioRepository.findByCorreo("capi.origen@brakket.local"))
                .thenReturn(Optional.of(capitanOrigen));
        lenient().when(usuarioRepository.findByCorreo("capi.destino@brakket.local"))
                .thenReturn(Optional.of(capitanDestino));
        lenient().when(transferenciaRepository.findById(100L)).thenReturn(Optional.of(solicitud));
        lenient().when(transferenciaRepository.save(any(SolicitudTransferencia.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private void plantillasParaEjecutar() {
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));
        when(miembroEquipoRepository.findByEquipoIdAndEstado(20L, "ACTIVO"))
                .thenReturn(List.of());
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(20L, 3L))
                .thenReturn(Optional.empty());
        when(miembroEquipoRepository.save(any(MiembroEquipo.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void la_primera_aceptacion_deja_la_solicitud_pendiente_de_la_otra_parte() {
        TransferenciaResponse resp =
                service.responder("jugador@brakket.local", 100L, ACEPTAR);

        assertThat(resp.estado()).isEqualTo("PENDIENTE");
        assertThat(resp.aprobacionJugador()).isEqualTo("ACEPTADA");
        assertThat(resp.aprobacionCapitanOrigen()).isEqualTo("PENDIENTE");
        verify(miembroEquipoRepository, never()).save(any());
    }

    @Test
    void con_ambas_aceptaciones_se_ejecuta_la_transferencia_y_se_actualiza_la_plantilla() {
        solicitud.setAprobacionJugador("ACEPTADA");
        plantillasParaEjecutar();

        TransferenciaResponse resp =
                service.responder("capi.origen@brakket.local", 100L, ACEPTAR);

        assertThat(resp.estado()).isEqualTo("APROBADA");
        assertThat(resp.fechaResolucion()).isNotNull();

        ArgumentCaptor<MiembroEquipo> miembros = ArgumentCaptor.forClass(MiembroEquipo.class);
        verify(miembroEquipoRepository, times(2)).save(miembros.capture());
        MiembroEquipo baja = miembros.getAllValues().get(0);
        MiembroEquipo alta = miembros.getAllValues().get(1);
        assertThat(baja.getEstado()).isEqualTo("TRANSFERIDO");
        assertThat(alta.getEquipo().getId()).isEqualTo(20L);
        assertThat(alta.getEstado()).isEqualTo("ACTIVO");
        assertThat(alta.getRol()).isEqualTo("TITULAR");

        // Notifica a jugador, capitán de origen y solicitante.
        verify(notificationService, times(3)).notificar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void un_rechazo_cancela_el_proceso_completo_y_notifica_a_las_partes() {
        TransferenciaResponse resp =
                service.responder("capi.origen@brakket.local", 100L, RECHAZAR);

        assertThat(resp.estado()).isEqualTo("RECHAZADA");
        assertThat(resp.aprobacionCapitanOrigen()).isEqualTo("RECHAZADA");
        verify(miembroEquipoRepository, never()).save(any());
        verify(notificationService, times(3)).notificar(any(), any(), any(), any(), any(), any());
    }

    @Test
    void responder_falla_si_la_solicitud_no_existe() {
        when(transferenciaRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.responder("jugador@brakket.local", 999L, ACEPTAR))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void responder_falla_si_la_solicitud_ya_fue_resuelta() {
        solicitud.setEstado("RECHAZADA");

        assertThatThrownBy(() -> service.responder("jugador@brakket.local", 100L, ACEPTAR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya fue resuelta");
    }

    @Test
    void responder_falla_si_el_usuario_no_es_parte_autorizada() {
        assertThatThrownBy(() -> service.responder("capi.destino@brakket.local", 100L, ACEPTAR))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void responder_falla_si_la_parte_ya_habia_respondido() {
        solicitud.setAprobacionJugador("ACEPTADA");

        assertThatThrownBy(() -> service.responder("jugador@brakket.local", 100L, ACEPTAR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Ya registraste");
    }

    @Test
    void la_ejecucion_falla_sin_modificar_plantilla_si_el_jugador_dejo_el_origen() {
        solicitud.setAprobacionJugador("ACEPTADA");
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.responder("capi.origen@brakket.local", 100L, ACEPTAR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya no pertenece");
        verify(miembroEquipoRepository, never()).save(any());
    }

    @Test
    void la_ejecucion_revalida_el_cupo_del_destino_al_completarse() {
        solicitud.setAprobacionJugador("ACEPTADA");
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));
        List<MiembroEquipo> llena = java.util.stream.IntStream
                .range(0, TransferRequestServiceImpl.CUPO_MAXIMO_PLANTILLA)
                .mapToObj(i -> MiembroEquipo.builder().estado("ACTIVO").build())
                .toList();
        when(miembroEquipoRepository.findByEquipoIdAndEstado(20L, "ACTIVO")).thenReturn(llena);

        assertThatThrownBy(() -> service.responder("capi.origen@brakket.local", 100L, ACEPTAR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cupo");
        verify(miembroEquipoRepository, never()).save(any());
    }

    @Test
    void la_ejecucion_falla_si_un_equipo_quedo_disuelto_o_bloqueado() {
        solicitud.setAprobacionJugador("ACEPTADA");
        destino.setEstado("BLOQUEADO");

        assertThatThrownBy(() -> service.responder("capi.origen@brakket.local", 100L, ACEPTAR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloqueado");
        verify(miembroEquipoRepository, never()).save(any());
    }

    @Test
    void la_ejecucion_reactiva_una_fila_previa_del_jugador_en_el_destino() {
        solicitud.setAprobacionJugador("ACEPTADA");
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));
        when(miembroEquipoRepository.findByEquipoIdAndEstado(20L, "ACTIVO"))
                .thenReturn(List.of());
        MiembroEquipo pasoPrevio = MiembroEquipo.builder()
                .id(77L).equipo(destino).usuario(jugador).estado("TRANSFERIDO").rol("SUPLENTE").build();
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(20L, 3L))
                .thenReturn(Optional.of(pasoPrevio));
        when(miembroEquipoRepository.save(any(MiembroEquipo.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.responder("capi.origen@brakket.local", 100L, ACEPTAR);

        assertThat(pasoPrevio.getEstado()).isEqualTo("ACTIVO");
        assertThat(pasoPrevio.getRol()).isEqualTo("TITULAR");
    }

    @Test
    void listarPendientes_devuelve_la_bandeja_de_la_parte_autorizada() {
        when(transferenciaRepository.findPendientesParaUsuario(3L))
                .thenReturn(List.of(solicitud));

        List<TransferenciaResponse> pendientes =
                service.listarPendientes("jugador@brakket.local");

        assertThat(pendientes).hasSize(1);
        assertThat(pendientes.get(0).jugadorNombre()).isEqualTo("Jugador Estrella");
    }
}
