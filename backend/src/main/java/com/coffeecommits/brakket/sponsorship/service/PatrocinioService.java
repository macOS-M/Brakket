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

    /**
     * Cascada Liga → Torneo: el patrocinio que corresponde mostrar para este
     * torneo. Si el torneo tiene uno propio vigente, ese gana; si no, y el
     * torneo pertenece a una liga, se usa el de la liga (si tiene uno vigente).
     * Vacío si no hay ninguno de los dos. Pensado para el panel comercial y
     * la pantalla de liga/torneo (pendientes de construir); ya se usa en
     * RF-50 para que el filtro por patrocinador encuentre torneos heredados.
     */
    Optional<PatrocinioResponse> resolverVigentePorTorneo(Long torneoId);
}