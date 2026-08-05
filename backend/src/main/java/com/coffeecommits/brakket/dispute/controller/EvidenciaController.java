package com.coffeecommits.brakket.dispute.controller;

import com.coffeecommits.brakket.dispute.dto.AdjuntarEvidenciaRequest;
import com.coffeecommits.brakket.dispute.dto.EvidenciaResponse;
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

    public EvidenciaController(EvidenciaService evidenciaService) {
        this.evidenciaService = evidenciaService;
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

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}