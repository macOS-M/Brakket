package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.EquipoBusquedaResponse;
import com.coffeecommits.brakket.team.dto.SolicitarUnionRequest;
import com.coffeecommits.brakket.team.dto.SolicitudUnionResponse;

import java.util.List;

/**
 * Solicitudes de unión a equipos (flujo inverso a la invitación) y el
 * listado "mis equipos" del usuario autenticado.
 */
public interface TeamJoinRequestService {

    /** Un jugador pide unirse a un equipo ajeno; se notifica al capitán. */
    SolicitudUnionResponse solicitar(Long equipoId, String correo, SolicitarUnionRequest request);

    /** Solicitudes pendientes de un equipo; solo su capitán las ve. */
    List<SolicitudUnionResponse> pendientesDeEquipo(Long equipoId, String correo);

    /** El capitán acepta (alta como TITULAR) o rechaza la solicitud. */
    SolicitudUnionResponse responder(Long solicitudId, String correo, boolean aceptar);

    /** Equipos donde el usuario autenticado es miembro activo. */
    List<EquipoBusquedaResponse> misEquipos(String correo);
}
