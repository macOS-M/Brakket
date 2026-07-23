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

        try {
            int importados = 0;
            for (var externo : externalGameSearchService.populares()) {
                if (juegoRepository.findByNombreIgnoreCase(externo.nombre()).isPresent()) {
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
            }
            if (importados > 0) {
                log.info("Catálogo sembrado con {} juegos populares de RAWG", importados);
            }
        } catch (Exception e) {
            log.warn("No se pudo sembrar el catálogo desde RAWG: {}", e.getMessage());
        }
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
