package com.coffeecommits.brakket.tournament.dto;

import com.coffeecommits.brakket.tournament.model.AjustePartida;
import com.coffeecommits.brakket.tournament.model.Torneo;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Torneo aplanado para el frontend, con la anatomía de la tarjeta de la
 * referencia: fecha, nombre, organizador, formato, tamaño, slots y estado.
 */
public record TorneoResponse(
        Long id,
        String nombre,
        String descripcion,
        Long juegoId,
        String juegoNombre,
        String juegoImagenUrl,
        Long ligaId,
        String ligaNombre,
        Long temporadaId,
        String temporadaNombre,
        Long organizadorId,
        String organizadorNombre,
        String formato,
        Integer tamanoEquipo,
        Integer maxEquipos,
        Long inscritos,
        LocalDateTime fechaInicio,
        String estado,
        Boolean publico,
        String premio,
        List<AjustePartida> ajustesPartida,
        Long campeonEquipoId,
        String campeonNombre
) {
    public static TorneoResponse from(Torneo torneo, long inscritos) {
        boolean deLiga = torneo.getTemporada() != null;
        return new TorneoResponse(
                torneo.getId(),
                torneo.getNombre(),
                torneo.getDescripcion(),
                torneo.getJuego().getId(),
                torneo.getJuego().getNombre(),
                torneo.getJuego().getImagenUrl(),
                deLiga ? torneo.getTemporada().getLiga().getId() : null,
                deLiga ? torneo.getTemporada().getLiga().getNombre() : null,
                deLiga ? torneo.getTemporada().getId() : null,
                deLiga ? torneo.getTemporada().getNombre() : null,
                torneo.getOrganizador().getId(),
                torneo.getOrganizador().getNombre(),
                torneo.getFormato(),
                torneo.getTamanoEquipo(),
                torneo.getMaxEquipos(),
                inscritos,
                torneo.getFechaInicio(),
                torneo.getEstado() == null ? null : torneo.getEstado().name(),
                torneo.getPublico(),
                torneo.getPremio(),
                torneo.getAjustesPartida() == null ? List.of() : torneo.getAjustesPartida(),
                torneo.getCampeon() == null ? null : torneo.getCampeon().getId(),
                torneo.getCampeon() == null ? null : torneo.getCampeon().getNombre()
        );
    }
}
