package com.coffeecommits.brakket.transmision.service;

import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Puerto de consulta de directos de una plataforma de streaming (RNF-23).
 *
 * <p>La página /transmisiones habla SOLO contra esta interfaz: añadir YouTube
 * o Kick es implementar otro provider, sin tocar el servicio ni la UI. Las
 * implementaciones lanzan {@code TwitchUnavailableException} (o su análoga)
 * cuando la plataforma no responde; el servicio degrada, nunca revienta.</p>
 */
public interface StreamProvider {

    PlataformaTransmision plataforma();

    /**
     * Datos de los canales indicados (existan o no en vivo).
     * Los handles que no existen simplemente no vienen en la respuesta.
     */
    List<CanalStream> getChannels(List<String> handles);

    /**
     * Directos activos entre los handles indicados. Un handle sin directo no
     * viene en la respuesta. Las implementaciones deben aceptar hasta 100
     * handles por consulta (límite de batch de Helix).
     */
    List<StreamEnVivo> getLiveStreams(List<String> handles);

    /** Último VOD publicado del canal (por id de usuario de la plataforma). */
    VodInfo getLatestVod(String userId);

    record CanalStream(String id, String login, String nombreMostrado,
                       String avatarUrl, String offlineImageUrl) {}

    record StreamEnVivo(String id, String userId, String login, String titulo,
                        int espectadores, String thumbnailUrl, String categoria,
                        String idioma, LocalDateTime iniciadoEn) {}

    record VodInfo(String id, String url, String titulo, String thumbnailUrl,
                   String duracion, LocalDateTime publicadoEn) {}
}
