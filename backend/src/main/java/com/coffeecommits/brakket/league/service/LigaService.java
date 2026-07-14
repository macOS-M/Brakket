package com.coffeecommits.brakket.league.service;

import com.coffeecommits.brakket.league.dto.ActualizarLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearTemporadaRequest;
import com.coffeecommits.brakket.league.dto.JuegoOpcionResponse;
import com.coffeecommits.brakket.league.dto.LigaResponse;
import com.coffeecommits.brakket.league.dto.TemporadaResponse;

import java.util.List;

/**
 * Lógica de creación y configuración de ligas y sus temporadas (RF-22, EPIC-07).
 *
 * <p>El {@code correoComisionado} identifica al usuario autenticado (lo aporta el
 * JWT). Quien crea una liga queda como su comisionado y es el único que puede
 * configurarla.</p>
 */
public interface LigaService {

    /** Crea una liga cuyo comisionado es el usuario autenticado. */
    LigaResponse crearLiga(String correoComisionado, CrearLigaRequest request);

    /** Lista todas las ligas (de la más reciente a la más antigua). */
    List<LigaResponse> listarLigas();

    /** Detalle de una liga por id (404 si no existe). */
    LigaResponse obtenerLiga(Long ligaId);

    /** Configura/edita una liga. Solo el comisionado puede hacerlo. */
    LigaResponse actualizarLiga(Long ligaId, String correoComisionado, ActualizarLigaRequest request);

    /** Temporadas de una liga. */
    List<TemporadaResponse> listarTemporadas(Long ligaId);

    /** Agrega una temporada a la liga. Solo el comisionado puede hacerlo. */
    TemporadaResponse crearTemporada(Long ligaId, String correoComisionado, CrearTemporadaRequest request);

    /** Juegos activos disponibles para asociar a una liga (selector del formulario). */
    List<JuegoOpcionResponse> listarJuegosDisponibles();
}
