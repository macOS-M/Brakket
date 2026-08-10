package com.coffeecommits.brakket.statistics.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record EstadisticasHistoricasResponse(
        String tipoSujeto,
        Long sujetoId,
        String sujetoNombre,
        FiltrosAplicados filtros,
        List<ResumenJuego> juegos,
        int resultadosNoDefinitivos
) {
    public record FiltrosAplicados(Long juegoId, Long temporadaId, Long ligaId,
                                   LocalDate desde, LocalDate hasta) {}

    public record ResumenJuego(Long juegoId, String juegoNombre, int partidas,
                               int victorias, int derrotas, BigDecimal porcentajeVictorias,
                               int torneos, List<Participacion> lineaTiempo) {}

    public record Participacion(Long torneoId, String torneoNombre, LocalDate fecha,
                                Long temporadaId, String temporadaNombre,
                                Long ligaId, String ligaNombre,
                                Long equipoId, String equipoNombre,
                                int partidas, int victorias, int derrotas,
                                BigDecimal porcentajeVictorias) {}
}
