package com.coffeecommits.brakket.team.dto;

import java.time.LocalDate;
import java.util.List;

/** Datos no privados que se pueden mostrar en el perfil público de un equipo. */
public record PerfilEquipoPublicoResponse(
        Long id,
        String nombre,
        String logo,
        String descripcion,
        String estado,
        Long capitanId,
        Long juegoId,
        String juegoNombre,
        List<String> redesSociales,
        List<IntegrantePublico> plantilla,
        List<TorneoRelacionado> torneos,
        EstadisticasGenerales estadisticas
) {
    public record IntegrantePublico(Long usuarioId, String nombre, String rol, LocalDate fechaUnion) {}

    public record TorneoRelacionado(Long id, String nombre, String estado, LocalDate fechaInicio,
                                    LocalDate fechaFin, String estadoInscripcion) {}

    public record EstadisticasGenerales(int victorias, int derrotas, int torneosJugados,
                                        boolean disponibles) {}
}
