package com.coffeecommits.brakket.game.controller;

import com.coffeecommits.brakket.game.dto.PerfilCompetitivoRequest;
import com.coffeecommits.brakket.game.dto.PerfilCompetitivoResponse;
import com.coffeecommits.brakket.game.service.CompetitiveProfileService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/competitive-profiles")
public class CompetitiveProfileController {

    private final CompetitiveProfileService service;

    public CompetitiveProfileController(
            CompetitiveProfileService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GESTIONAR_TORNEOS')")
    public PerfilCompetitivoResponse crear(
            @Valid @RequestBody PerfilCompetitivoRequest request) {

        return service.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_TORNEOS')")
    public PerfilCompetitivoResponse actualizar(@PathVariable Long id,
                                                @Valid @RequestBody PerfilCompetitivoRequest request) {
        return service.actualizar(id, request);
    }

    @GetMapping("/{id}")
    public PerfilCompetitivoResponse obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @GetMapping("/game/{juegoId}")
    public PerfilCompetitivoResponse obtenerPorJuego(@PathVariable Long juegoId) {
        return service.obtenerPorJuego(juegoId);
    }
}
