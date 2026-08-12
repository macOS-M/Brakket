package com.coffeecommits.brakket.progression.dto;
import java.time.LocalDate;
import java.util.List;

public record ProgresionResponse(int puntos, List<LogroResponse> logros, List<ElementoResponse> elementos) {
    public record LogroResponse(Long id, String nombre, String descripcion, int puntos, String origen,
                                boolean desbloqueado, LocalDate fecha) {}
    public record ElementoResponse(Long id, String nombre, String descripcion, String tipo, int costo,
                                   boolean activo, boolean canjeado, boolean aplicado, boolean habilitado,
                                   String requisito) {}
}
