package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.transfer.model.HistorialTransferencia;
import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;
import com.coffeecommits.brakket.transfer.repository.HistorialTransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HistorialTransferenciaServiceImpl implements HistorialTransferenciaService {

    private final HistorialTransferenciaRepository historialRepository;

    public HistorialTransferenciaServiceImpl(HistorialTransferenciaRepository historialRepository) {
        this.historialRepository = historialRepository;
    }

    @Override
    @Transactional
    public void registrar(SolicitudTransferencia solicitud, Usuario responsable) {

        if (!SolicitudTransferencia.ESTADO_APROBADA.equals(solicitud.getEstado())) {
            throw new BusinessException(
                    "Solo se registra en el historial una transferencia aprobada (estado actual: %s)"
                            .formatted(solicitud.getEstado()));
        }

        if (historialRepository.existsBySolicitudId(solicitud.getId())) {
            return;
        }

        historialRepository.save(HistorialTransferencia.builder()
                .solicitudId(solicitud.getId())
                .jugador(solicitud.getJugador())
                .equipoOrigen(solicitud.getEquipoOrigen())
                .equipoDestino(solicitud.getEquipoDestino())
                .rolAsignado(solicitud.getRolPropuesto())
                .responsable(responsable)
                .build());
    }
}