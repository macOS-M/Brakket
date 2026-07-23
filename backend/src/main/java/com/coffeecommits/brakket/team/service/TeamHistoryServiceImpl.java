package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.HistorialEquipoResponse;
import com.coffeecommits.brakket.team.dto.MovimientoPlantillaResponse;
import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.team.model.MiembroEquipo;
import com.coffeecommits.brakket.team.repository.EquipoRepository;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import com.coffeecommits.brakket.transfer.model.HistorialTransferencia;
import com.coffeecommits.brakket.transfer.repository.HistorialTransferenciaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class TeamHistoryServiceImpl implements TeamHistoryService {

    private final EquipoRepository equipoRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;
    private final HistorialTransferenciaRepository historialTransferenciaRepository;

    public TeamHistoryServiceImpl(EquipoRepository equipoRepository,
                                  MiembroEquipoRepository miembroEquipoRepository,
                                  HistorialTransferenciaRepository historialTransferenciaRepository) {
        this.equipoRepository = equipoRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
        this.historialTransferenciaRepository = historialTransferenciaRepository;
    }

    /**
     * Consulta pública de lectura: cualquier usuario autenticado puede ver
     * el historial de cualquier equipo (perfil público del equipo), salvo el
     * detalle de jugadores con perfil privado, que se filtra por movimiento.
     * No requiere ser capitán ni miembro del equipo.
     */
    @Override
    @Transactional(readOnly = true)
    public HistorialEquipoResponse obtenerHistorial(Long equipoId, String actorCorreo,
                                                    LocalDate desde, LocalDate hasta) {

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new IllegalArgumentException(
                    "El filtro de fechas no es válido: 'desde' no puede ser posterior a 'hasta'.");
        }

        Equipo equipo = equipoRepository.findById(equipoId)
                .orElseThrow(() -> new ResourceNotFoundException("Equipo", equipoId));

        List<MovimientoPlantillaResponse> movimientos = new ArrayList<>();

        // Altas y bajas: cada fila de miembro_equipo es una etapa de la
        // relación jugador-equipo (ACTIVO, TRANSFERIDO, EXPULSADO).
        List<MiembroEquipo> membresias = miembroEquipoRepository.findByEquipoId(equipoId);
        for (MiembroEquipo m : membresias) {

            movimientos.add(new MovimientoPlantillaResponse(
                    "ALTA",
                    m.getFechaUnion().atStartOfDay(),
                    m.getUsuario().getId(),
                    m.getUsuario().getNombre(),
                    esPublico(m.getUsuario()),
                    m.getRol(),
                    null, null, null, null, null,
                    null, null
            ));

            if ("EXPULSADO".equals(m.getEstado()) && m.getFechaBaja() != null) {
                movimientos.add(new MovimientoPlantillaResponse(
                        "BAJA",
                        m.getFechaBaja(),
                        m.getUsuario().getId(),
                        m.getUsuario().getNombre(),
                        esPublico(m.getUsuario()),
                        null,
                        m.getCausaBaja(),
                        null, null, null, null,
                        m.getResponsableBaja() != null ? m.getResponsableBaja().getId() : null,
                        m.getResponsableBaja() != null ? m.getResponsableBaja().getNombre() : null
                ));
            }
        }

        // Transferencias: el equipo participó como origen o como destino.
        List<HistorialTransferencia> transferencias = historialTransferenciaRepository
                .findByEquipoOrigenIdOrEquipoDestinoIdOrderByFechaTransferenciaDesc(equipoId, equipoId);
        for (HistorialTransferencia t : transferencias) {
            movimientos.add(new MovimientoPlantillaResponse(
                    "TRANSFERENCIA",
                    t.getFechaTransferencia(),
                    t.getJugador().getId(),
                    t.getJugador().getNombre(),
                    esPublico(t.getJugador()),
                    t.getRolAsignado(),
                    null,
                    t.getEquipoOrigen().getId(),
                    t.getEquipoOrigen().getNombre(),
                    t.getEquipoDestino().getId(),
                    t.getEquipoDestino().getNombre(),
                    t.getResponsable().getId(),
                    t.getResponsable().getNombre()
            ));
        }

        // Filtro opcional por rango de fechas (criterio RF-16: "Datos de
        // Entrada: ...rango de fechas opcional").
        if (desde != null) {
            movimientos.removeIf(mv -> mv.fecha().toLocalDate().isBefore(desde));
        }
        if (hasta != null) {
            movimientos.removeIf(mv -> mv.fecha().toLocalDate().isAfter(hasta));
        }

        // Orden cronológico: más reciente primero (criterio "ordenar por fecha").
        movimientos.sort(Comparator.comparing(MovimientoPlantillaResponse::fecha).reversed());

        return new HistorialEquipoResponse(
                equipo.getId(),
                equipo.getNombre(),
                equipo.getEstado(),
                movimientos
        );
    }

    /**
     * "Perfil público" según Usuario.visibilidadPerfil (RF-18). Si es PRIVATE,
     * el frontend no debe ofrecer navegar al detalle del jugador (criterio
     * RF-16: "acceder al detalle... cuando la información sea pública").
     */
    private Boolean esPublico(Usuario usuario) {
        return usuario.getVisibilidadPerfil() == null
                || "PUBLIC".equals(usuario.getVisibilidadPerfil().name());
    }
}