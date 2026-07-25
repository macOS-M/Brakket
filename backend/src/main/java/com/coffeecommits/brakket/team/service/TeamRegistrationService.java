package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.CrearEquipoRequest;
import com.coffeecommits.brakket.team.dto.EditarEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;

public interface TeamRegistrationService {

    /**
     * Crea el equipo. Un jugador queda como capitán y miembro; un ADMIN
     * debe designar por correo al jugador que será capitán (el
     * administrador no forma parte del equipo).
     */
    EquipoResponse crear(CrearEquipoRequest request, String creadorCorreo, boolean esAdmin);

    /** Edición parcial: capitán activo del equipo, o ADMIN (moderación). */
    EquipoResponse editar(Long equipoId, EditarEquipoRequest request,
                          String actorCorreo, boolean esAdmin);

    EquipoResponse obtenerPorId(Long equipoId);
}