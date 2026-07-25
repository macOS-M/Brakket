package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.transfer.model.HistorialTransferencia;
import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;
import com.coffeecommits.brakket.transfer.repository.HistorialTransferenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HistorialTransferenciaServiceImplTest {

    @Mock
    private HistorialTransferenciaRepository historialRepository;

    @InjectMocks
    private HistorialTransferenciaServiceImpl service;

    private Usuario jugador;
    private Usuario responsable;
    private Equipo origen;
    private Equipo destino;
    private SolicitudTransferencia solicitudAprobada;

    @BeforeEach
    void setUp() {
        jugador = Usuario.builder().id(3L).nombre("Jugador Estrella").build();
        responsable = Usuario.builder().id(2L).nombre("Capi Origen").build();
        origen = Equipo.builder().id(10L).nombre("Origen FC").build();
        destino = Equipo.builder().id(20L).nombre("Destino FC").build();

        solicitudAprobada = SolicitudTransferencia.builder()
                .id(100L)
                .jugador(jugador)
                .equipoOrigen(origen)
                .equipoDestino(destino)
                .rolPropuesto("TITULAR")
                .estado(SolicitudTransferencia.ESTADO_APROBADA)
                .build();
    }

    @Test
    void registrar_crea_el_historial_de_una_transferencia_aprobada() {
        when(historialRepository.existsBySolicitudId(100L)).thenReturn(false);

        service.registrar(solicitudAprobada, responsable);

        ArgumentCaptor<HistorialTransferencia> captor =
                ArgumentCaptor.forClass(HistorialTransferencia.class);
        verify(historialRepository).save(captor.capture());

        HistorialTransferencia h = captor.getValue();
        assertThat(h.getSolicitudId()).isEqualTo(100L);
        assertThat(h.getJugador()).isEqualTo(jugador);
        assertThat(h.getEquipoOrigen()).isEqualTo(origen);
        assertThat(h.getEquipoDestino()).isEqualTo(destino);
        assertThat(h.getRolAsignado()).isEqualTo("TITULAR");
        assertThat(h.getResponsable()).isEqualTo(responsable);
    }

    @Test
    void registrar_falla_si_la_solicitud_no_esta_aprobada() {
        solicitudAprobada.setEstado(SolicitudTransferencia.ESTADO_PENDIENTE);

        assertThatThrownBy(() -> service.registrar(solicitudAprobada, responsable))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("aprobada");
        verify(historialRepository, never()).save(any());
    }

    @Test
    void registrar_no_duplica_si_ya_existe_un_registro_para_la_solicitud() {
        when(historialRepository.existsBySolicitudId(100L)).thenReturn(true);

        service.registrar(solicitudAprobada, responsable);

        verify(historialRepository, never()).save(any());
    }
}