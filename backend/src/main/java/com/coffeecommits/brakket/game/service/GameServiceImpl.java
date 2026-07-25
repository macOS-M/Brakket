package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.game.dto.JuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoResponse;
import com.coffeecommits.brakket.game.model.Juego;
import com.coffeecommits.brakket.game.repository.JuegoRepository;
import com.coffeecommits.brakket.league.repository.LigaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GameServiceImpl implements GameService {

    private final JuegoRepository juegoRepository;
    private final LigaRepository ligaRepository;
    private final ExternalGameSearchService externalGameSearchService;

    public GameServiceImpl(JuegoRepository juegoRepository,
                           LigaRepository ligaRepository,
                           ExternalGameSearchService externalGameSearchService) {
        this.juegoRepository = juegoRepository;
        this.ligaRepository = ligaRepository;
        this.externalGameSearchService = externalGameSearchService;
    }

    @Override
    @Transactional
    public JuegoResponse crear(JuegoRequest request) {
        juegoRepository.findByNombre(request.nombre()).ifPresent(j -> {
            throw new BusinessException(
                    "Ya existe un juego con el nombre '%s'".formatted(request.nombre()));
        });

        Juego juego = Juego.builder()
                .nombre(request.nombre())
                .genero(request.genero())
                .descripcion(request.descripcion())
                .imagenUrl(request.imagenUrl())
                .activo(true)
                .build();

        return JuegoResponse.fromEntity(juegoRepository.save(juego));
    }

    @Override
    @Transactional
    public JuegoResponse editar(Long id, JuegoRequest request) {
        Juego juego = juegoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Juego", id));

        juegoRepository.findByNombre(request.nombre())
                .filter(otro -> !otro.getId().equals(id))
                .ifPresent(otro -> {
                    throw new BusinessException(
                            "Ya existe un juego con el nombre '%s'".formatted(request.nombre()));
                });

        juego.setNombre(request.nombre());
        juego.setGenero(request.genero());
        juego.setDescripcion(request.descripcion());
        juego.setImagenUrl(request.imagenUrl());

        return JuegoResponse.fromEntity(juegoRepository.save(juego));
    }

    @Override
    @Transactional(readOnly = true)
    public List<JuegoResponse> listarActivos() {
        return juegoRepository.findByActivoTrue().stream()
                .map(JuegoResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public JuegoResponse obtenerPorId(Long id) {
        Juego juego = juegoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Juego", id));
        return JuegoResponse.fromEntity(juego);
    }

    @Override
    @Transactional
    public JuegoResponse importarDesdeExterno(String nombre, boolean esAdmin) {
        String buscado = nombre == null ? "" : nombre.trim();
        if (buscado.isEmpty()) {
            throw new BusinessException("Indicá el nombre del juego a importar");
        }

        // Idempotente: si ya está en el catálogo se devuelve sin volver a
        // consultar la API.
        Juego existente = juegoRepository.findByNombreIgnoreCase(buscado).orElse(null);
        if (existente != null) {
            return reactivarSiCorresponde(existente, esAdmin);
        }

        var resultados = externalGameSearchService.buscar(buscado);
        var elegido = resultados.stream()
                .filter(r -> r.nombre().equalsIgnoreCase(buscado))
                .findFirst()
                .orElseGet(() -> resultados.isEmpty() ? null : resultados.get(0));
        if (elegido == null) {
            throw new BusinessException(
                    "No se encontró '%s' en el catálogo externo".formatted(buscado));
        }

        // El resultado elegido puede diferir del texto buscado; se rechequea
        // contra el catálogo propio con el nombre definitivo.
        Juego porNombreFinal = juegoRepository.findByNombreIgnoreCase(elegido.nombre()).orElse(null);
        if (porNombreFinal != null) {
            return reactivarSiCorresponde(porNombreFinal, esAdmin);
        }

        Juego juego = Juego.builder()
                .nombre(elegido.nombre())
                .genero(elegido.genero() == null || elegido.genero().isBlank()
                        ? "Sin clasificar"
                        : elegido.genero())
                .imagenUrl(elegido.imagenUrl())
                .rawgSlug(elegido.slug())
                .activo(true)
                .build();
        enriquecerDesdeRawg(juego, elegido.slug());
        return JuegoResponse.fromEntity(juegoRepository.save(juego));
    }

    /**
     * Un juego desactivado fue curado fuera por un ADMIN (DD-01): un usuario
     * común no lo resucita al importarlo, pero un ADMIN sí (y de paso se le
     * completa la ficha RAWG si le faltaba).
     */
    private JuegoResponse reactivarSiCorresponde(Juego existente, boolean esAdmin) {
        if (!Boolean.TRUE.equals(existente.getActivo())) {
            if (!esAdmin) {
                throw new BusinessException(
                        "El título '%s' fue desactivado por un administrador".formatted(existente.getNombre()));
            }
            existente.setActivo(true);
            if (existente.getRating() == null) {
                enriquecerPorNombre(existente);
            }
            juegoRepository.save(existente);
        }
        return JuegoResponse.fromEntity(existente);
    }

    /** Resuelve el slug buscando por nombre (juegos previos a V28 sin slug). */
    private void enriquecerPorNombre(Juego juego) {
        try {
            String slug = juego.getRawgSlug();
            if (slug == null) {
                slug = externalGameSearchService.buscar(juego.getNombre()).stream()
                        .filter(r -> r.nombre().equalsIgnoreCase(juego.getNombre()))
                        .map(r -> r.slug())
                        .findFirst().orElse(null);
                juego.setRawgSlug(slug);
            }
            enriquecerDesdeRawg(juego, slug);
        } catch (Exception e) {
            // Sin ficha no se bloquea nada: el juego queda con datos básicos.
        }
    }

    /**
     * Ficha completa desde RAWG en el momento del import (una sola vez por
     * juego; después todo sale de la BD sin gastar cuota). Nunca bloquea:
     * si RAWG falla, el juego entra con los datos básicos.
     */
    private void enriquecerDesdeRawg(Juego juego, String slug) {
        var detalle = externalGameSearchService.detalle(slug);
        if (detalle == null) {
            return;
        }
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

    @Override
    @Transactional
    public void desactivar(Long id) {
        Juego juego = juegoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Juego", id));

        if (ligaRepository.existsByJuegoId(id)) {
            throw new BusinessException(
                    "No se puede desactivar el juego '%s': tiene ligas asociadas."
                            .formatted(juego.getNombre()));
        }

        juego.setActivo(false);
        juegoRepository.save(juego);
    }
}