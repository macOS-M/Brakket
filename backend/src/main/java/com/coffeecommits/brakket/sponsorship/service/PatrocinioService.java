package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinioRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinioResponse;

import java.util.List;
import java.util.Optional;

public interface PatrocinioService {

    PatrocinioResponse crear(CrearPatrocinioRequest request);

    PatrocinioResponse obtenerPorId(Long id);

    List<PatrocinioResponse> listarPorTorneo(Long torneoId);

    List<PatrocinioResponse> listarPorLiga(Long ligaId);

    List<PatrocinioResponse> listarPorTemporada(Long temporadaId);

    List<PatrocinioResponse> listarTodos();

    Optional<PatrocinioResponse> resolverVigentePorTorneo(Long torneoId);

    /**
     * Elimina el patrocinio. Sus espacios publicitarios se borran en cascada
     * a nivel de base de datos (ON DELETE CASCADE, migración V55) — no hace
     * falta borrarlos aparte acá.
     */
    void eliminar(Long id);
}