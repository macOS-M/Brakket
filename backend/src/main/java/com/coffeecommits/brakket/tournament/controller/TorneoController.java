package com.coffeecommits.brakket.tournament.controller;

import com.coffeecommits.brakket.tournament.dto.CrearTorneoRequest;
import com.coffeecommits.brakket.tournament.dto.EquipoElegibleResponse;
import com.coffeecommits.brakket.tournament.dto.InscribirEquipoRequest;
import com.coffeecommits.brakket.tournament.dto.PartidaResponse;
import com.coffeecommits.brakket.tournament.dto.ReportarResultadoRequest;
import com.coffeecommits.brakket.tournament.dto.TorneoDetalleResponse;
import com.coffeecommits.brakket.tournament.dto.TorneoResponse;
import com.coffeecommits.brakket.tournament.service.PartidaService;
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
import com.coffeecommits.brakket.tournament.dto.CasoEspecialResponse;
import com.coffeecommits.brakket.tournament.dto.RegistrarCasoEspecialRequest;

/**
 * API de torneos (RF-24/RF-25) con modelo abierto de organizadores:
 * cualquier usuario autenticado crea; gestiona el organizador o un ADMIN.
 * Las lecturas de torneos públicos no exigen sesión (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/tournaments")
public class TorneoController {

    private final TorneoService torneoService;
    private final PartidaService partidaService;

    public TorneoController(TorneoService torneoService, PartidaService partidaService) {
        this.torneoService = torneoService;
        this.partidaService = partidaService;
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

    /** "Tus competencias": los que organizo + donde compite mi equipo. */
    @GetMapping("/mios")
    @PreAuthorize("isAuthenticated()")
    public List<TorneoResponse> misCompetencias(Authentication authentication) {
        return torneoService.misCompetencias(authentication.getName());
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
        return torneoService.inscribirEquipo(id, authentication.getName(), request.equipoId(),
                request.usuarioEnJuego());
    }

    // ---------- torneo en vivo (RF-26/27/29 mínimos) ----------

    /** Cierra inscripciones, genera el bracket y pone el torneo en curso. */
    @PostMapping("/{id}/iniciar")
    @PreAuthorize("isAuthenticated()")
    public List<PartidaResponse> iniciar(@PathVariable Long id, Authentication authentication) {
        return partidaService.iniciarTorneo(id, authentication.getName(), esAdmin(authentication));
    }

    /** El bracket completo; público si el torneo es público. */
    @GetMapping("/{id}/bracket")
    public List<PartidaResponse> bracket(@PathVariable Long id, Authentication authentication) {
        return partidaService.obtenerBracket(id, correoDe(authentication), esAdmin(authentication));
    }

    /** Un capitán de la partida reporta el marcador. */
    @PostMapping("/partidas/{partidaId}/reporte")
    @PreAuthorize("isAuthenticated()")
    public PartidaResponse reportar(@PathVariable Long partidaId,
                                    @Valid @RequestBody ReportarResultadoRequest request,
                                    Authentication authentication) {
        return partidaService.reportar(partidaId, authentication.getName(), request);
    }

    /** El capitán rival confirma; el ganador avanza en la llave. */
    @PostMapping("/partidas/{partidaId}/confirmacion")
    @PreAuthorize("isAuthenticated()")
    public PartidaResponse confirmar(@PathVariable Long partidaId, Authentication authentication) {
        return partidaService.confirmar(partidaId, authentication.getName());
    }

    /** El capitán rival rechaza el reporte: queda en disputa. */
    @PostMapping("/partidas/{partidaId}/rechazo")
    @PreAuthorize("isAuthenticated()")
    public PartidaResponse rechazar(@PathVariable Long partidaId, Authentication authentication) {
        return partidaService.rechazar(partidaId, authentication.getName());
    }

    /** El organizador (o un ADMIN) fija el resultado final. */
    @PostMapping("/partidas/{partidaId}/resolucion")
    @PreAuthorize("isAuthenticated()")
    public PartidaResponse resolver(@PathVariable Long partidaId,
                                    @Valid @RequestBody ReportarResultadoRequest request,
                                    Authentication authentication) {
        return partidaService.resolver(partidaId, authentication.getName(),
                esAdmin(authentication), request);
    }



    // RF-28: comisionado (organizador), arbitro del torneo, o ADMIN registran
    // un descanso, avance automatico o abandono sobre una partida especifica.
    @PostMapping("/partidas/{partidaId}/caso-especial")
    @PreAuthorize("isAuthenticated()")
    public PartidaResponse registrarCasoEspecial(@PathVariable Long partidaId,
                                                 @Valid @RequestBody RegistrarCasoEspecialRequest request,
                                                 Authentication authentication) {
        return partidaService.registrarCasoEspecial(partidaId, authentication.getName(),
                esAdmin(authentication), request);
    }

    // RF-28: historial de descansos/avances/abandonos de la partida. Mismo
    // criterio de acceso que registrarlos: no es informacion publica.
    @GetMapping("/partidas/{partidaId}/casos-especiales")
    @PreAuthorize("isAuthenticated()")
    public List<CasoEspecialResponse> historialCasoEspecial(@PathVariable Long partidaId,
                                                            Authentication authentication) {
        return partidaService.historialCasoEspecial(partidaId, authentication.getName(),
                esAdmin(authentication));
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
