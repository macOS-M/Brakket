package com.coffeecommits.brakket.team.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // Los torneos arrancan a una hora concreta desde el modelo abierto (V22).
    public record TorneoRelacionado(Long id, String nombre, String estado, LocalDateTime fechaInicio,
                                    LocalDateTime fechaFin, String estadoInscripcion) {}

    public record EstadisticasGenerales(int victorias, int derrotas, int torneosJugados,
                                        boolean disponibles) {}
}
