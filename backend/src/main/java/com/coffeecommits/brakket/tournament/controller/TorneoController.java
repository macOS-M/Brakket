package com.coffeecommits.brakket.tournament.controller;

import com.coffeecommits.brakket.tournament.dto.CrearTorneoRequest;
import com.coffeecommits.brakket.tournament.dto.EquipoElegibleResponse;
import com.coffeecommits.brakket.tournament.dto.InscribirEquipoRequest;
import com.coffeecommits.brakket.tournament.dto.TorneoDetalleResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoResponse;
import com.coffeecommits.brakket.tournament.service.TorneoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de torneos (RF-24/RF-25) con modelo abierto de organizadores:
 * cualquier usuario autenticado crea; gestiona el organizador o un ADMIN.
 * Las lecturas de torneos públicos no exigen sesión (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/tournaments")
public class TorneoController {

    private final TorneoService torneoService;

    public TorneoController(TorneoService torneoService) {
        this.torneoService = torneoService;
    }

    @PostMapping
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public TorneoResponse crear(@Valid @RequestBody CrearTorneoRequest request,
                                Authentication authentication) {
        return torneoService.crearTorneo(authentication.getName(), esAdmin(authentication), request);
    }

    /** Torneos visibles, opcionalmente filtrados por juego. */
    @GetMapping
    public List<TorneoResponse> listar(@RequestParam(value = "juegoId", required = false) Long juegoId,
                                       Authentication authentication) {
        return torneoService.listar(juegoId, correoDe(authentication));
    }

    @GetMapping("/{id}")
    public TorneoDetalleResponse obtener(@PathVariable Long id, Authentication authentication) {
        return torneoService.obtenerDetalle(id, correoDe(authentication), esAdmin(authentication));
    }

    /** Equipos del capitán autenticado elegibles para inscribirse. */
    @GetMapping("/{id}/equipos-elegibles")
    @PreAuthorize("isAuthenticated()")
    public List<EquipoElegibleResponse> equiposElegibles(@PathVariable Long id,
                                                         Authentication authentication) {
        return torneoService.equiposElegibles(id, authentication.getName());
    }

    @PostMapping("/{id}/inscripciones")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.CREATED)
    public TorneoDetalleResponse inscribir(@PathVariable Long id,
                                           @Valid @RequestBody InscribirEquipoRequest request,
                                           Authentication authentication) {
        return torneoService.inscribirEquipo(id, authentication.getName(), request.equipoId());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, Authentication authentication) {
        torneoService.eliminarTorneo(id, authentication.getName(), esAdmin(authentication));
    }

    private static String correoDe(Authentication authentication) {
        return authentication == null ? null : authentication.getName();
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
