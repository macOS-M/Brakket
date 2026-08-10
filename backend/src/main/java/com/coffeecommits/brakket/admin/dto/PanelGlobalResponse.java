package com.coffeecommits.brakket.admin.dto;

import java.util.List;

/**
 * Carga completa del panel global de administración (RF-49): los conteos de la
 * plataforma más la actividad de auditoría más reciente, en una sola respuesta.
 */
public record PanelGlobalResponse(
        ResumenPlataformaResponse resumen,
        List<LogAuditoriaResponse> actividadReciente
) {
}
