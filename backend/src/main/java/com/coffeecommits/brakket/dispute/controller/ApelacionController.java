package com.coffeecommits.brakket.dispute.controller;

import com.coffeecommits.brakket.dispute.dto.ApelacionResponse;
import com.coffeecommits.brakket.dispute.dto.ApelarRequest;
import com.coffeecommits.brakket.dispute.dto.ResolverApelacionRequest;
import com.coffeecommits.brakket.dispute.service.ApelacionService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class ApelacionController {

    private final ApelacionService apelacionService;

    public ApelacionController(ApelacionService apelacionService) {
        this.apelacionService = apelacionService;
    }

    /** RF-32: apelar una disputa ya resuelta, dentro del plazo. */
    @PostMapping("/disputas/{disputaId}/apelaciones")
    @PreAuthorize("isAuthenticated()")
    public ApelacionResponse apelar(@PathVariable Long disputaId,
                                    @Valid @RequestBody ApelarRequest request,
                                    Authentication authentication) {
        return apelacionService.apelar(disputaId, authentication.getName(), esAdmin(authentication), request);
    }

    /** Para que el frontend sepa si hay una apelación (y su ID/estado). */
    @GetMapping("/disputas/{disputaId}/apelaciones")
    @PreAuthorize("isAuthenticated()")
    public java.util.List<ApelacionResponse> listarPorDisputa(@PathVariable Long disputaId,
                                                              Authentication authentication) {
        return apelacionService.listarPorDisputa(disputaId, authentication.getName(), esAdmin(authentication));
    }

    /** Solo el comisionado de la liga (o admin) resuelve la apelación. */
    @PostMapping("/apelaciones/{apelacionId}/resolucion")
    @PreAuthorize("isAuthenticated()")
    public ApelacionResponse resolver(@PathVariable Long apelacionId,
                                      @jakarta.validation.Valid @RequestBody ResolverApelacionRequest request,
                                      Authentication authentication) {
        return apelacionService.resolver(apelacionId, authentication.getName(), esAdmin(authentication), request);
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}