package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.auth.model.VisibilidadPerfil;
import com.coffeecommits.brakket.auth.repository.UsuarioRepository;
import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ForbiddenException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.team.dto.HistorialEquipoJugadorResponse;
import com.coffeecommits.brakket.team.repository.MiembroEquipoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class PlayerHistoryServiceImpl implements PlayerHistoryService {

    private final UsuarioRepository usuarioRepository;
    private final MiembroEquipoRepository miembroEquipoRepository;

    public PlayerHistoryServiceImpl(UsuarioRepository usuarioRepository,
                                    MiembroEquipoRepository miembroEquipoRepository) {
        this.usuarioRepository = usuarioRepository;
        this.miembroEquipoRepository = miembroEquipoRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<HistorialEquipoJugadorResponse> historial(Long jugadorId, Long juegoId, LocalDate desde,
                                                          LocalDate hasta, String correoSolicitante,
                                                          boolean esAdmin) {
        Usuario jugador = usuarioRepository.findById(jugadorId)
                .orElseThrow(() -> new ResourceNotFoundException("Usuario", jugadorId));

        boolean esElMismo = correoSolicitante != null && correoSolicitante.equals(jugador.getCorreo());
        boolean perfilPrivado = jugador.getVisibilidadPerfil() == VisibilidadPerfil.PRIVATE;

        if (perfilPrivado && !esElMismo && !esAdmin) {
            throw new ForbiddenException("Este jugador mantiene su historial en privado");
        }

        if (desde != null && hasta != null && desde.isAfter(hasta)) {
            throw new BusinessException("El rango de fechas no es valido");
        }

        return miembroEquipoRepository.historialDeJugador(jugadorId, juegoId, desde, hasta).stream()
                .map(HistorialEquipoJugadorResponse::fromEntity)
                .toList();
    }
}