package com.coffeecommits.brakket.game.config;

import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.game.service.ExternalGameSearchService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

/**
 * Puebla el catálogo con los títulos más populares de RAWG al arrancar,
 * para que la página de Juegos nunca esté vacía (referencia Challenger
 * Mode: el catálogo se despliega solo, nadie lo carga a mano).
 *
 * <p>Solo corre si hay API key configurada y el catálogo activo es chico;
 * es idempotente por nombre y nunca tumba el arranque: si RAWG no responde
 * se registra y la app sigue.</p>
 */
@Component
public class RawgCatalogSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(RawgCatalogSeeder.class);

    /** Por debajo de este número de juegos activos se completa el catálogo. */
    private static final int MINIMO_CATALOGO = 8;

    /**
     * Catálogo curado: los títulos competitivos reales (guía: el catálogo
     * de Challenger Mode). El "-added" de RAWG traía juegos populares pero
     * no competitivos (Skyrim, Portal, The Witcher…).
     */
    private static final java.util.List<String> CURADOS = java.util.List.of(
            "League of Legends", "Valorant", "Counter-Strike 2", "Dota 2",
            "Fortnite", "Rocket League", "Overwatch 2", "Apex Legends",
            "Tom Clancy's Rainbow Six Siege", "PUBG: Battlegrounds",
            "Call of Duty: Warzone", "Street Fighter 6", "Tekken 8",
            "Mortal Kombat 1", "EA Sports FC 25", "Brawlhalla",
            "Hearthstone", "Halo Infinite", "Fall Guys: Ultimate Knockout",
            "Clash Royale");

    private final JuegoRepository juegoRepository;
    private final ExternalGameSearchService externalGameSearchService;

    public RawgCatalogSeeder(JuegoRepository juegoRepository,
                             ExternalGameSearchService externalGameSearchService) {
        this.juegoRepository = juegoRepository;
        this.externalGameSearchService = externalGameSearchService;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!externalGameSearchService.disponible()) {
            return;
        }
        if (juegoRepository.findByActivoTrue().size() >= MINIMO_CATALOGO) {
            completarFichasFaltantes();
            return;
        }

        // Try por título: un fallo de RAWG (rate limit, timeout) no debe
        // abortar el resto del catálogo.
        int importados = 0;
        for (String nombre : CURADOS) {
            try {
                var existente = juegoRepository.findByNombreIgnoreCase(nombre).orElse(null);
                if (existente != null) {
                    // Curado ya presente: se reactiva y, si su arte era el
                    // stock de las migraciones viejas, se refresca con RAWG.
                    existente.setActivo(true);
                    if (existente.getRating() == null || esArteStock(existente.getImagenUrl())) {
                        var refresco = externalGameSearchService.buscar(nombre).stream()
                                .findFirst().orElse(null);
                        if (refresco != null) {
                            existente.setImagenUrl(refresco.imagenUrl());
                            existente.setRawgSlug(refresco.slug());
                        }
                    }
                    juegoRepository.save(existente);
                    continue;
                }
                var externo = externalGameSearchService.buscar(nombre).stream()
                        .findFirst().orElse(null);
                if (externo == null) {
                    continue;
                }
                Juego juego = Juego.builder()
                        .nombre(externo.nombre())
                        .genero(externo.genero() == null || externo.genero().isBlank()
                                ? "Sin clasificar"
                                : externo.genero())
                        .imagenUrl(externo.imagenUrl())
                        .rawgSlug(externo.slug())
                        .activo(true)
                        .build();
                // Ficha completa (descripción, rating, capturas…) al sembrar;
                // si falla para un título, ese entra con los datos básicos.
                try {
                    var detalle = externalGameSearchService.detalle(externo.slug());
                    if (detalle != null) {
                        juego.setDescripcion(detalle.descripcion());
                        juego.setFechaLanzamiento(detalle.fechaLanzamiento());
                        juego.setRating(detalle.rating());
                        juego.setMetacritic(detalle.metacritic());
                        juego.setSitioWeb(detalle.sitioWeb());
                        juego.setPlataformas(detalle.plataformas().isEmpty()
                                ? null : String.join(" · ", detalle.plataformas()));
                        juego.setEtiquetas(detalle.etiquetas().isEmpty()
                                ? null : String.join(", ", detalle.etiquetas()));
                        juego.setCapturas(detalle.capturas());
                    }
                } catch (Exception e) {
                    log.debug("Sin ficha RAWG para {}: {}", externo.nombre(), e.getMessage());
                }
                juegoRepository.save(juego);
                importados++;
            } catch (Exception e) {
                log.warn("No se pudo sembrar '{}' desde RAWG: {}", nombre, e.getMessage());
            }
        }
        if (importados > 0) {
            log.info("Catálogo sembrado con {} juegos competitivos de RAWG", importados);
        }
        completarFichasFaltantes();
    }

    /** Arte de las migraciones semilla o del fallback viejo del frontend. */
    private static boolean esArteStock(String url) {
        return url == null || url.contains("pexels.com");
    }

    /**
     * Backfill: juegos sembrados antes de V28 quedaron sin ficha (rating,
     * capturas…). Se completan al arrancar, una sola vez cada uno.
     */
    private void completarFichasFaltantes() {
        try {
            int completados = 0;
            for (Juego juego : juegoRepository.findByActivoTrue()) {
                if (juego.getRating() != null || completados >= 30) {
                    continue;
                }
                String slug = juego.getRawgSlug();
                if (slug == null) {
                    slug = externalGameSearchService.buscar(juego.getNombre()).stream()
                            .filter(r -> r.nombre().equalsIgnoreCase(juego.getNombre()))
                            .map(r -> r.slug())
                            .findFirst().orElse(null);
                }
                var detalle = externalGameSearchService.detalle(slug);
                if (detalle == null) {
                    continue;
                }
                juego.setRawgSlug(slug);
                juego.setDescripcion(juego.getDescripcion() == null
                        ? detalle.descripcion() : juego.getDescripcion());
                juego.setFechaLanzamiento(detalle.fechaLanzamiento());
                juego.setRating(detalle.rating());
                juego.setMetacritic(detalle.metacritic());
                juego.setSitioWeb(detalle.sitioWeb());
                juego.setPlataformas(detalle.plataformas().isEmpty()
                        ? null : String.join(" · ", detalle.plataformas()));
                juego.setEtiquetas(detalle.etiquetas().isEmpty()
                        ? null : String.join(", ", detalle.etiquetas()));
                juego.setCapturas(detalle.capturas());
                juegoRepository.save(juego);
                completados++;
            }
            if (completados > 0) {
                log.info("Ficha RAWG completada para {} juegos del catálogo", completados);
            }
        } catch (Exception e) {
            log.warn("No se pudieron completar fichas desde RAWG: {}", e.getMessage());
        }
    }
}
