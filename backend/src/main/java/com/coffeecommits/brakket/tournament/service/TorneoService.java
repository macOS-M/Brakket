package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.tournament.dto.CrearTorneoRequest;
import com.coffeecommits.brakket.tournament.dto.EquipoElegibleResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoDetalleResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoResponse;

import java.util.List;

/**
 * Torneos con modelo abierto de organizadores (RF-24/RF-25, decisión de
 * diseño inspirada en Challenger Mode): cualquier usuario autenticado crea;
 * gestiona solo el organizador (o un ADMIN). El capitán inscribe su equipo.
 */
public interface TorneoService {

    /** Crea un torneo; el usuario autenticado queda como organizador. */
    TorneoResponse crearTorneo(String correo, boolean esAdmin, CrearTorneoRequest request);

    /**
     * Torneos visibles de un juego (o de todos, con juegoId null): los
     * públicos más los privados del propio usuario cuando hay sesión.
     */
    List<TorneoResponse> listar(Long juegoId, String correoOpcional);

    /** Detalle con equipos inscritos. Privado: solo organizador o ADMIN. */
    TorneoDetalleResponse obtenerDetalle(Long torneoId, String correoOpcional, boolean esAdmin);

    /** Inscribe un equipo del capitán autenticado (RF-25). */
    TorneoDetalleResponse inscribirEquipo(Long torneoId, String correo, Long equipoId);

    /** Equipos del capitán autenticado que pueden inscribirse en este torneo. */
    List<EquipoElegibleResponse> equiposElegibles(Long torneoId, String correo);

    /** Elimina el torneo con sus inscripciones. Solo organizador o ADMIN. */
    void eliminarTorneo(Long torneoId, String correo, boolean esAdmin);
}
