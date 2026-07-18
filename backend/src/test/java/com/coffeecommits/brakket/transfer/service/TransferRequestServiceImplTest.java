package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.notification.model.Notificacion;
import com.coffeecommits.brakket.notification.repository.NotificacionRepository;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.transfer.dto.CrearTransferenciaRequest;
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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TransferRequestServiceImplTest {

    @Mock
    private SolicitudTransferenciaRepository transferenciaRepository;
    @Mock
    private EquipoRepository equipoRepository;
    @Mock
    private MiembroEquipoRepository miembroEquipoRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private NotificacionRepository notificacionRepository;
    @InjectMocks
    private TransferRequestServiceImpl service;

    private Usuario capitanDestino;
    private Usuario capitanOrigen;
    private Usuario jugador;
    private Equipo origen;
    private Equipo destino;

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

        lenient().when(usuarioRepository.findByCorreo("capi.destino@brakket.local"))
                .thenReturn(Optional.of(capitanDestino));
        lenient().when(equipoRepository.findById(20L)).thenReturn(Optional.of(destino));
        lenient().when(equipoRepository.findById(10L)).thenReturn(Optional.of(origen));
        lenient().when(usuarioRepository.findById(3L)).thenReturn(Optional.of(jugador));
    }

    private CrearTransferenciaRequest requestValido() {
        return new CrearTransferenciaRequest(3L, 10L, 20L, "TITULAR", "Refuerzo para el split");
    }

    private void jugadorActivoEnOrigen() {
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(20L, 3L))
                .thenReturn(Optional.empty());
        when(miembroEquipoRepository.findByEquipoIdAndEstado(20L, "ACTIVO"))
                .thenReturn(List.of());
    }

    @Test
    void solicitar_crea_la_solicitud_pendiente_y_notifica_a_las_partes() {
        jugadorActivoEnOrigen();
        when(transferenciaRepository.existsByJugadorIdAndEstado(3L, "PENDIENTE")).thenReturn(false);
        when(transferenciaRepository.save(any(SolicitudTransferencia.class)))
                .thenAnswer(inv -> {
                    SolicitudTransferencia s = inv.getArgument(0);
                    s.setId(100L);
                    return s;
                });

        TransferenciaResponse resp = service.solicitar("capi.destino@brakket.local", requestValido());

        assertThat(resp.estado()).isEqualTo("PENDIENTE");
        assertThat(resp.aprobacionJugador()).isEqualTo("PENDIENTE");
        assertThat(resp.aprobacionCapitanOrigen()).isEqualTo("PENDIENTE");
        assertThat(resp.rolPropuesto()).isEqualTo("TITULAR");

        ArgumentCaptor<Notificacion> notif = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionRepository, times(2)).save(notif.capture());
        assertThat(notif.getAllValues())
                .extracting(n -> n.getUsuario().getId())
                .containsExactlyInAnyOrder(3L, 2L); // jugador y capitán de origen
    }

    @Test
    void solicitar_falla_si_el_solicitante_no_es_capitan_del_destino() {
        when(usuarioRepository.findByCorreo("otro@brakket.local"))
                .thenReturn(Optional.of(Usuario.builder().id(99L).nombre("Otro").build()));

        assertThatThrownBy(() -> service.solicitar("otro@brakket.local", requestValido()))
                .isInstanceOf(ForbiddenException.class);
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_si_origen_y_destino_son_el_mismo_equipo() {
        var request = new CrearTransferenciaRequest(3L, 20L, 20L, "TITULAR", null);

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", request))
                .isInstanceOf(IllegalArgumentException.class);
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_si_el_jugador_no_pertenece_al_origen() {
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no pertenece");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_si_el_jugador_es_el_capitan_del_origen() {
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("CAPITAN").build()));

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("capitán");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_si_el_destino_no_tiene_cupo() {
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(20L, 3L))
                .thenReturn(Optional.empty());
        List<MiembroEquipo> plantillaLlena = java.util.stream.IntStream
                .range(0, TransferRequestServiceImpl.CUPO_MAXIMO_PLANTILLA)
                .mapToObj(i -> MiembroEquipo.builder().estado("ACTIVO").build())
                .toList();
        when(miembroEquipoRepository.findByEquipoIdAndEstado(20L, "ACTIVO"))
                .thenReturn(plantillaLlena);

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("cupo");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_si_ya_hay_una_transferencia_pendiente_del_jugador() {
        jugadorActivoEnOrigen();
        when(transferenciaRepository.existsByJugadorIdAndEstado(3L, "PENDIENTE")).thenReturn(true);

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("pendiente");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_si_un_equipo_esta_disuelto_o_bloqueado() {
        origen.setEstado("DISUELTO");

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("disuelto");

        origen.setEstado("ACTIVO");
        destino.setEstado("BLOQUEADO");
        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("bloqueado");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void solicitar_falla_con_rol_propuesto_invalido() {
        var request = new CrearTransferenciaRequest(3L, 10L, 20L, "MASCOTA", null);

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Rol propuesto inválido");
        verify(transferenciaRepository, never()).save(any());
    }

    @Test
    void listarEnviadas_devuelve_el_seguimiento_del_solicitante() {
        SolicitudTransferencia s = SolicitudTransferencia.builder()
                .id(100L).jugador(jugador).equipoOrigen(origen).equipoDestino(destino)
                .solicitante(capitanDestino).rolPropuesto("TITULAR").build();
        when(transferenciaRepository.findBySolicitanteIdOrderByFechaSolicitudDesc(1L))
                .thenReturn(List.of(s));

        List<TransferenciaResponse> enviadas =
                service.listarEnviadas("capi.destino@brakket.local");

        assertThat(enviadas).hasSize(1);
        assertThat(enviadas.get(0).equipoDestinoNombre()).isEqualTo("Destino FC");
        assertThat(enviadas.get(0).estado()).isEqualTo("PENDIENTE");
    }

    @Test
    void solicitar_falla_si_el_jugador_ya_esta_en_el_destino() {
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(10L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(origen).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));
        when(miembroEquipoRepository.findByEquipoIdAndUsuarioId(20L, 3L))
                .thenReturn(Optional.of(MiembroEquipo.builder()
                        .equipo(destino).usuario(jugador).estado("ACTIVO").rol("TITULAR").build()));

        assertThatThrownBy(() -> service.solicitar("capi.destino@brakket.local", requestValido()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("ya forma parte");
        verify(transferenciaRepository, never()).save(any());
    }
}
