package com.coffeecommits.brakket.game.controller;

import com.coffeecommits.brakket.game.dto.JuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoResponse;
import com.coffeecommits.brakket.game.service.GameService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Endpoints del catálogo de juegos (RF-20).
 *
 * <p>Lecturas (GET) públicas — ver SecurityConfig ({@code /api/games/**}).
 * Escrituras requieren JWT válido. Restricción por rol (ADMIN/COMISIONADO)
 * pendiente de RF-19: {@link com.coffeecommits.brakket.config.JwtAuthenticationFilter}
 * aún no carga los roles del usuario en el contexto de seguridad.</p>
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
    @ResponseStatus(HttpStatus.CREATED)
    public JuegoResponse crear(@Valid @RequestBody JuegoRequest request) {
        return gameService.crear(request);
    }

    @PutMapping("/{id}")
    public JuegoResponse editar(@PathVariable Long id, @Valid @RequestBody JuegoRequest request) {
        return gameService.editar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        gameService.desactivar(id);
    }
}
