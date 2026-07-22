package com.coffeecommits.brakket.game.controller;

import com.coffeecommits.brakket.game.dto.JuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoResponse;
import com.coffeecommits.brakket.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del catálogo de juegos (RF-20).
 *
 * <p>Lecturas (GET) públicas — ver SecurityConfig ({@code /api/games/**}).
 * Las escrituras exigen el permiso {@code GESTIONAR_TORNEOS} (ADMIN y
 * COMISIONADO), el mismo que protege los perfiles competitivos: administrar
 * el catálogo es configurar sobre qué se compite.</p>
 */
@RestController
@RequestMapping("/api/games")
public class GameController {

    private final GameService gameService;

    public GameController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping
    public List<JuegoResponse> listar() {
        return gameService.listarActivos();
    }

    @GetMapping("/{id}")
    public JuegoResponse obtener(@PathVariable Long id) {
        return gameService.obtenerPorId(id);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('GESTIONAR_TORNEOS')")
    @ResponseStatus(HttpStatus.CREATED)
    public JuegoResponse crear(@Valid @RequestBody JuegoRequest request) {
        return gameService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_TORNEOS')")
    public JuegoResponse editar(@PathVariable Long id, @Valid @RequestBody JuegoRequest request) {
        return gameService.editar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasAuthority('GESTIONAR_TORNEOS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        gameService.desactivar(id);
    }
}
