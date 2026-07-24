package com.coffeecommits.brakket.twitch.controller;

import com.coffeecommits.brakket.twitch.dto.*;
import com.coffeecommits.brakket.twitch.service.CanalTwitchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/twitch")
@RequiredArgsConstructor
public class CanalTwitchController {
    private final CanalTwitchService service;

    @GetMapping
    public ResponseEntity<CanalTwitchResponse> obtener() { return ResponseEntity.ok(service.obtener()); }

    @PostMapping("/canal")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CanalTwitchResponse> configurar(@Valid @RequestBody ConfigurarCanalTwitchRequest request) {
        return ResponseEntity.ok(service.configurar(request));
    }

    @PostMapping("/validar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CanalTwitchResponse> validar() { return ResponseEntity.ok(service.validar()); }

    @PostMapping("/transmisiones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransmisionTwitchResponse> asociar(@RequestBody AsociarTransmisionRequest request) {
        return ResponseEntity.ok(service.asociar(request));
    }

    /** RF-36: indicadores de audiencia capturados para una transmisión. */
    @GetMapping("/transmisiones/{id}/metricas")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<MetricasTransmisionResponse> metricas(@PathVariable Long id) {
        return ResponseEntity.ok(service.metricas(id));
    }
}

