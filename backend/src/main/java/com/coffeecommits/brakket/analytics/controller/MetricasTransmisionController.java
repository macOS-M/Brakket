package com.coffeecommits.brakket.analytics.controller;

import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse;
import com.coffeecommits.brakket.analytics.dto.TransmisionAnalizableResponse;
import com.coffeecommits.brakket.analytics.service.MetricasTransmisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;

/** RF-37: panel analítico de transmisiones. */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class MetricasTransmisionController {

    private final MetricasTransmisionService service;

    /** Transmisiones que el usuario puede consultar; sin esto no hay forma de obtener un id. */
    @GetMapping("/transmisiones")
    @PreAuthorize("hasAnyRole('ADMIN','COMISIONADO')")
    public ResponseEntity<List<TransmisionAnalizableResponse>> transmisiones(Authentication authentication) {
        return ResponseEntity.ok(service.catalogo(authentication.getName(), esAdmin(authentication)));
    }

    @GetMapping("/transmisiones/{transmisionId}/series")
    @PreAuthorize("hasAnyRole('ADMIN','COMISIONADO')")
    public ResponseEntity<SeriesTransmisionResponse> series(
            @PathVariable Long transmisionId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta,
            @RequestParam(required = false) String agrupacion,
            Authentication authentication) {
        return ResponseEntity.ok(service.series(transmisionId, desde, hasta, agrupacion,
                authentication.getName(), esAdmin(authentication)));
    }

    private static boolean esAdmin(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
    }
}
