package com.coffeecommits.brakket.game.controller;

import com.coffeecommits.brakket.game.dto.ImportarJuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoExternoResponse;
import com.coffeecommits.brakket.game.dto.JuegoRequest;
import com.coffeecommits.brakket.game.dto.JuegoResponse;
import com.coffeecommits.brakket.game.service.ExternalGameSearchService;
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
    private final ExternalGameSearchService externalGameSearchService;

    public GameController(GameService gameService,
                          ExternalGameSearchService externalGameSearchService) {
        this.gameService = gameService;
        this.externalGameSearchService = externalGameSearchService;
    }

    @GetMapping
    public List<JuegoResponse> listar() {
        return gameService.listarActivos();
    }

    @GetMapping("/{id}")
    public JuegoResponse obtener(@PathVariable Long id) {
        return gameService.obtenerPorId(id);
    }

    /**
     * Busca juegos en el catálogo externo (RAWG). Lo usa el catálogo para
     * ofrecer títulos que todavía no están localmente: cualquier usuario con
     * sesión puede buscar (la key queda protegida detrás del login; la ruta
     * se excluye del permitAll de GET /api/games/** en SecurityConfig).
     */
    /**
     * Los juegos top reales de RAWG (no dependen del catálogo local): la
     * fila "Juegos top" del panel. Cacheado 1 h para cuidar la cuota.
     */
    @GetMapping("/top")
    public List<JuegoExternoResponse> topExterno() {
        try {
            return externalGameSearchService.populares();
        } catch (Exception e) {
            return List.of();
        }
    }

    @GetMapping("/buscar-externo")
    @PreAuthorize("isAuthenticated()")
    public List<JuegoExternoResponse> buscarExterno(@RequestParam("q") String consulta) {
        return externalGameSearchService.buscar(consulta);
    }

    /**
     * Trae un juego del catálogo externo al propio (idempotente). Los datos
     * salen de RAWG, no del usuario, así que no hay riesgo de datos basura:
     * cualquier usuario con sesión puede sumar el título donde quiere
     * competir (referencia Challenger Mode).
     */
    @PostMapping("/importar-externo")
    @PreAuthorize("isAuthenticated()")
    public JuegoResponse importarExterno(@Valid @RequestBody ImportarJuegoRequest request,
                                         org.springframework.security.core.Authentication authentication) {
        boolean esAdmin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        return gameService.importarDesdeExterno(request.nombre(), esAdmin);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    public JuegoResponse crear(@Valid @RequestBody JuegoRequest request) {
        return gameService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public JuegoResponse editar(@PathVariable Long id, @Valid @RequestBody JuegoRequest request) {
        return gameService.editar(id, request);
    }

    @PatchMapping("/{id}/desactivar")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void desactivar(@PathVariable Long id) {
        gameService.desactivar(id);
    }
}
