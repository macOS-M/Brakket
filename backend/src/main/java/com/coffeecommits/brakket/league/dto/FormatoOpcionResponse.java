package com.coffeecommits.brakket.league.dto;

import com.coffeecommits.brakket.game.model.FormatoCompetitivo;

public record FormatoOpcionResponse(Long id, String nombre) {
    public static FormatoOpcionResponse from(FormatoCompetitivo formato) {
        return new FormatoOpcionResponse(formato.getId(), formato.getNombre());
    }
}
