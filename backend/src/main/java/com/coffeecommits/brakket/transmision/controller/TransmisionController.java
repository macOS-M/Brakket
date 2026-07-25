package com.coffeecommits.brakket.transmision.controller;

import com.coffeecommits.brakket.transmision.dto.TransmisionesResponse;
import com.coffeecommits.brakket.transmision.service.TransmisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Transmisiones en vivo de Brakket (RF-35). Lectura pública: ver los directos
 * no exige sesión, igual que el catálogo de torneos. El frontend consume SOLO
 * este endpoint; las credenciales de Twitch jamás salen del backend (RNF-13).
 */
@RestController
@RequestMapping("/api/transmisiones")
@RequiredArgsConstructor
public class TransmisionController {
    private final TransmisionService service;

    @GetMapping
    public ResponseEntity<TransmisionesResponse> listar() {
        return ResponseEntity.ok(service.listar());
    }
}
