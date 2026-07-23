package com.coffeecommits.brakket.team.dto;

import com.coffeecommits.brakket.auth.model.Usuario;

public record JugadorDisponibleResponse(
        Long id,
        String nombre,
        String fotoUrl,
        boolean requiereTransferencia,
        String equipoActualNombre
) {

    public static JugadorDisponibleResponse invitacionDirecta(Usuario usuario) {
        return new JugadorDisponibleResponse(
                usuario.getId(), usuario.getNombre(), usuario.getFotoUrl(), false, null);
    }

    public static JugadorDisponibleResponse conTransferencia(Usuario usuario, String equipoActualNombre) {
        return new JugadorDisponibleResponse(
                usuario.getId(), usuario.getNombre(), usuario.getFotoUrl(), true, equipoActualNombre);
    }
}
