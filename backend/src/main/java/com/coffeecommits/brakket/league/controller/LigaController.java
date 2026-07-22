package com.coffeecommits.brakket.league.controller;

import com.coffeecommits.brakket.league.dto.ActualizarLigaRequest;
import com.coffeecommits.brakket.league.dto.ActualizarTemporadaRequest;
import com.coffeecommits.brakket.league.dto.FormatoOpcionResponse;
import com.coffeecommits.brakket.league.dto.CrearLigaRequest;
import com.coffeecommits.brakket.league.dto.CrearTemporadaRequest;
import com.coffeecommits.brakket.league.dto.JuegoOpcionResponse;
import com.coffeecommits.brakket.league.dto.LigaResponse;
import com.coffeecommits.brakket.league.dto.TemporadaResponse;
import com.coffeecommits.brakket.league.service.LigaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
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
 * <p>Todas las rutas requieren JWT válido (ver {@code SecurityConfig}). El
 * comisionado de cada acción se resuelve a partir del usuario autenticado
 * ({@code Authentication#getName()} = correo), no del cuerpo de la petición.</p>
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
    public LigaResponse actualizar(@PathVariable Long id,
                                   @Valid @RequestBody ActualizarLigaRequest request,
                                   Authentication authentication) {
        return ligaService.actualizarLiga(id, authentication.getName(), request);
    }

    /** Temporadas de una liga. */
    @GetMapping("/{id}/seasons")
    public List<TemporadaResponse> temporadas(@PathVariable Long id) {
        return ligaService.listarTemporadas(id);
    }

    /** Agrega una temporada a la liga (solo su comisionado). */
    @PostMapping("/{id}/seasons")
    @ResponseStatus(HttpStatus.CREATED)
    public TemporadaResponse crearTemporada(@PathVariable Long id,
                                            @Valid @RequestBody CrearTemporadaRequest request,
                                            Authentication authentication) {
        return ligaService.crearTemporada(id, authentication.getName(), request);
    }

    @PutMapping("/{id}/seasons/{temporadaId}")
    public TemporadaResponse actualizarTemporada(@PathVariable Long id,
                                                 @PathVariable Long temporadaId,
                                                 @Valid @RequestBody ActualizarTemporadaRequest request,
                                                 Authentication authentication) {
        return ligaService.actualizarTemporada(id, temporadaId, authentication.getName(), request);
    }

    @GetMapping("/{id}/season-formats")
    public List<FormatoOpcionResponse> formatos(@PathVariable Long id) {
        return ligaService.listarFormatosDisponibles(id);
    }
}
