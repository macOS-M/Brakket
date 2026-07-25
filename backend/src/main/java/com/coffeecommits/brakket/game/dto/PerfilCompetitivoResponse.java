package com.coffeecommits.brakket.game.dto;

import java.util.List;

public record PerfilCompetitivoResponse(

        Long id,
        Long juegoId,
        String juego,
        String modalidad,
        Integer plantillaMinima,
        Integer plantillaMaxima,
        List<String> formatos,
        List<String> estadisticas,
        List<Long> formatosIds,
        List<Long> estadisticasIds,
        Boolean activo,
        String mensaje

) {
}
