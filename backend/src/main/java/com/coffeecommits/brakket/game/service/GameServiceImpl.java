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

    public GameServiceImpl(JuegoRepository juegoRepository, LigaRepository ligaRepository) {
        this.juegoRepository = juegoRepository;
        this.ligaRepository = ligaRepository;
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