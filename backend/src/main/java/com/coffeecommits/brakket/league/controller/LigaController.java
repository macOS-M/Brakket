package com.coffeecommits.brakket.league.controller;

import com.coffeecommits.brakket.league.dto.ActualizarLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearTemporadaRequest;
import com.coffeecommits.brakket.league.dto.JuegoOpcionResponse;
import com.coffeecommits.brakket.league.dto.LigaResponse;
import com.coffeecommits.brakket.league.dto.TemporadaResponse;
import com.coffeecommits.brakket.league.service.LigaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de ligas y temporadas (RF-22, EPIC-07).
 *
 * <p>Las lecturas son públicas (ver {@code SecurityConfig}); las escrituras
 * exigen el permiso {@code GESTIONAR_LIGAS} (ADMIN y COMISIONADO). Además,
 * el service valida la propiedad: solo el comisionado de la liga puede
 * modificarla (el ADMIN puede eliminar cualquier liga). El usuario se
 * resuelve del JWT ({@code Authentication#getName()} = correo), no del
 * cuerpo de la petición.</p>
 */
@RestController
@RequestMapping("/api/leagues")
public class LigaController {

    private final LigaService ligaService;

    public LigaController(LigaService ligaService) {
        this.ligaService = ligaService;
    }

    /** Crea una liga; el usuario autenticado queda como comisionado. */
    @PostMapping
    @PreAuthorize("hasAuthority('GESTIONAR_LIGAS')")
    @ResponseStatus(HttpStatus.CREATED)
    public LigaResponse crear(@Valid @RequestBody CrearLigaRequest request,
                              Authentication authentication) {
        return ligaService.crearLiga(authentication.getName(), request);
    }

    /** Lista todas las ligas. */
    @GetMapping
    public List<LigaResponse> listar() {
        return ligaService.listarLigas();
    }

    /** Juegos activos disponibles para el selector del formulario de liga. */
    @GetMapping("/game-options")
    public List<JuegoOpcionResponse> juegosDisponibles() {
        return ligaService.listarJuegosDisponibles();
    }

    /** Detalle de una liga. */
    @GetMapping("/{id}")
    public LigaResponse obtener(@PathVariable Long id) {
        return ligaService.obtenerLiga(id);
    }

    /** Configura/edita una liga (solo su comisionado). */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_LIGAS')")
    public LigaResponse actualizar(@PathVariable Long id,
                                   @Valid @RequestBody ActualizarLigaRequest request,
                                   Authentication authentication) {
        return ligaService.actualizarLiga(id, authentication.getName(), request);
    }

    /**
     * Elimina una liga con sus temporadas (RF-22). Puede hacerlo su
     * comisionado o un ADMIN de la plataforma.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_LIGAS')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void eliminar(@PathVariable Long id, Authentication authentication) {
        boolean esAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        ligaService.eliminarLiga(id, authentication.getName(), esAdmin);
    }

    /** Temporadas de una liga. */
    @GetMapping("/{id}/seasons")
    public List<TemporadaResponse> temporadas(@PathVariable Long id) {
        return ligaService.listarTemporadas(id);
    }

    /** Agrega una temporada a la liga (solo su comisionado). */
    @PostMapping("/{id}/seasons")
    @PreAuthorize("hasAuthority('GESTIONAR_LIGAS')")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaResponse crearTemporada(@PathVariable Long id,
                                            @Valid @RequestBody CrearTemporadaRequest request,
                                            Authentication authentication) {
        return ligaService.crearTemporada(id, authentication.getName(), request);
    }
}
