package com.coffeecommits.brakket.progression.controller;

import com.coffeecommits.brakket.progression.dto.PerfilPersonalizadoResponse;
import com.coffeecommits.brakket.progression.service.ProgressionService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/players")
public class PublicProgressionController {
    private final ProgressionService service;
    public PublicProgressionController(ProgressionService service) { this.service=service; }

    @GetMapping("/{jugadorId}/customization")
    public PerfilPersonalizadoResponse perfil(@PathVariable Long jugadorId) {
        return service.perfilPublico(jugadorId);
    }
}
