package com.coffeecommits.brakket.twitch.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.coffeecommits.brakket.twitch.dto.AsociarTransmisionRequest;
import com.coffeecommits.brakket.twitch.dto.CanalTwitchResponse;
import com.coffeecommits.brakket.twitch.dto.ConfigurarCanalTwitchRequest;
import com.coffeecommits.brakket.twitch.dto.MetricasTransmisionResponse;
import com.coffeecommits.brakket.twitch.dto.TransmisionTwitchResponse;
import com.coffeecommits.brakket.twitch.service.CanalTwitchService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.List;

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

    /** RF-34: transmisiones abiertas, para que el panel recupere el estado al cargar. */
    @GetMapping("/transmisiones")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<TransmisionTwitchResponse>> abiertas() {
        return ResponseEntity.ok(service.listarAbiertas());
    }

    /** RF-34: cerrar el período de captura sin esperar a que el directo termine. */
    @PostMapping("/transmisiones/{id}/finalizar")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<TransmisionTwitchResponse> finalizar(@PathVariable Long id) {
        return ResponseEntity.ok(service.finalizar(id));
    }
}

