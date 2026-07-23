package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.game.dto.JuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoResponse;

import java.util.List;

/**
 * Lógica de gestión del catálogo de juegos (RF-20).
 */
public interface GameService {

    /** Crea un juego nuevo. Falla si el nombre ya existe. */
    JuegoResponse crear(JuegoRequest request);

    /** Edita un juego existente. Falla si el nuevo nombre ya lo usa otro juego. */
    JuegoResponse editar(Long id, JuegoRequest request);

    /** Devuelve el catálogo completo de juegos activos. */
    List<JuegoResponse> listarActivos();

    /** Devuelve un juego por id (activo o no). */
    JuegoResponse obtenerPorId(Long id);

    /** Desactiva un juego (baja lógica, nunca se borra de la base). */
    void desactivar(Long id);

    /**
     * Trae un juego del catálogo externo al propio (o lo reactiva/devuelve si
     * ya existía). Los datos salen de la API, no del usuario: cualquier
     * usuario autenticado puede pedirlo sin riesgo de datos basura.
     */
    JuegoResponse importarDesdeExterno(String nombre);
}