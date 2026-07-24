package com.coffeecommits.brakket.transmision.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Respuesta de GET /api/transmisiones (RF-35).
 *
 * <p>{@code actualizadoEn} es la hora de la última consulta EXITOSA a la
 * plataforma: con {@code degradado=true} la UI la usa para avisar "última
 * información de hace X minutos" en vez de presentar datos viejos como
 * actuales (RNF-15).</p>
 */
public record TransmisionesResponse(
        List<TransmisionResponse> transmisiones,
        LocalDateTime actualizadoEn,
        boolean degradado) {

    public record TransmisionResponse(
            String plataforma,
            String loginCanal,
            String nombreCanal,
            String avatarUrl,
            String urlCanal,
            /* EN_VIVO | OFFLINE | DESCONOCIDO — el relleno "próximamente" de la
               grilla es presentación pura y vive solo en el frontend. */
            String estado,
            String titulo,
            Integer espectadores,
            String thumbnailUrl,
            String categoria,
            String idioma,
            LocalDateTime iniciadaEn,
            boolean destacada,
            Long torneoId,
            String nombreTorneo,
            VodResponse vod) {}

    public record VodResponse(
            String id,
            String url,
            String titulo,
            String thumbnailUrl,
            String duracion,
            LocalDateTime publicadoEn) {}
}
