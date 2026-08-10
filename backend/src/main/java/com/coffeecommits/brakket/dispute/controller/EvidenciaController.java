package com.coffeecommits.brakket.dispute.controller;

import com.coffeecommits.brakket.dispute.dto.AdjuntarEvidenciaRequest;
import com.coffeecommits.brakket.dispute.dto.EvidenciaResponse;
import com.coffeecommits.brakket.dispute.service.DisputaService;
import com.coffeecommits.brakket.dispute.service.EvidenciaService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/disputas")
public class EvidenciaController {

    private final EvidenciaService evidenciaService;
    private final DisputaService disputaService;

    public EvidenciaController(EvidenciaService evidenciaService, DisputaService disputaService) {
        this.evidenciaService = evidenciaService;
        this.disputaService = disputaService;
    }

    /** RF-31: adjuntar evidencia (archivo ya subido a /api/uploads, o un enlace). */
    @PostMapping("/{disputaId}/evidencias")
    @PreAuthorize("isAuthenticated()")
    public EvidenciaResponse adjuntar(@PathVariable Long disputaId,
                                      @Valid @RequestBody AdjuntarEvidenciaRequest request,
                                      Authentication authentication) {
        return evidenciaService.adjuntar(disputaId, authentication.getName(), esAdmin(authentication), request);
    }
    @GetMapping("/{disputaId}/evidencias")
    @PreAuthorize("isAuthenticated()")
    public List<EvidenciaResponse> listar(@PathVariable Long disputaId, Authentication authentication) {
        return evidenciaService.listar(disputaId, authentication.getName(), esAdmin(authentication));
    }

    /** RF-32: árbitro del torneo, comisionado de la liga, o admin. */
    @PostMapping("/{disputaId}/resolucion")
    @PreAuthorize("isAuthenticated()")
    public com.coffeecommits.brakket.dispute.dto.DisputaResponse resolver(
            @PathVariable Long disputaId,
            @Valid @RequestBody com.coffeecommits.brakket.dispute.dto.ResolverDisputaRequest request,
            Authentication authentication) {
        return disputaService.resolver(disputaId, authentication.getName(), esAdmin(authentication), request);
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}