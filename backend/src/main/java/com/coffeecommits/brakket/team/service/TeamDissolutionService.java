package com.coffeecommits.brakket.team.service;

import com.coffeecommits.brakket.team.dto.DisolverEquipoRequest;
import com.coffeecommits.brakket.team.dto.EquipoResponse;

/**
 * Ciclo de vida del equipo (RF-03): disolución lógica, reactivación y
 * eliminación definitiva.
 */
public interface TeamDissolutionService {

    /**
     * Disuelve un equipo activo. Lo hace el capitán del equipo (o un ADMIN
     * como moderación) y el equipo no debe tener participación competitiva
     * pendiente (inscripciones activas o partidas sin resolver).
     */
    EquipoResponse disolver(Long equipoId, DisolverEquipoRequest request,
                            String solicitanteCorreo, boolean esAdmin);

    /**
     * Reactiva un equipo disuelto: vuelve a ACTIVO y limpia los datos de la
     * disolución. Lo hace el capitán del equipo o un ADMIN. Un equipo
     * BLOQUEADO no se reactiva por esta vía (es una medida administrativa).
     */
    EquipoResponse reactivar(Long equipoId, String solicitanteCorreo, boolean esAdmin);

    /**
     * Elimina el equipo definitivamente para que no estorbe en las
     * búsquedas. El capitán puede eliminar su equipo ya disuelto; un ADMIN
     * puede eliminar cualquier equipo. Si el equipo tiene historial
     * competitivo (inscripciones, partidas, títulos o transferencias) el
     * borrado se rechaza: ese historial se conserva disolviéndolo.
     */
    void eliminar(Long equipoId, String solicitanteCorreo, boolean esAdmin);
}
