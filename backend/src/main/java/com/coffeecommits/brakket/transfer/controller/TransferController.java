package com.coffeecommits.brakket.transfer.controller;

import com.coffeecommits.brakket.transfer.dto.CrearTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;
import com.coffeecommits.brakket.transfer.service.TransferRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API de transferencias de jugadores (RF-12, EPIC-03). Todas las rutas
 * requieren JWT válido; el solicitante se resuelve del usuario autenticado.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferController {

    private final TransferRequestService transferRequestService;

    public TransferController(TransferRequestService transferRequestService) {
        this.transferRequestService = transferRequestService;
    }

    /** Crea una solicitud de transferencia (solo el capitán del equipo destino). */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TransferenciaResponse solicitar(@Valid @RequestBody CrearTransferenciaRequest request,
                                           Authentication authentication) {
        return transferRequestService.solicitar(authentication.getName(), request);
    }

    /** Seguimiento del equipo solicitante: solicitudes iniciadas por el usuario. */
    @GetMapping("/enviadas")
    public List<TransferenciaResponse> enviadas(Authentication authentication) {
        return transferRequestService.listarEnviadas(authentication.getName());
    }
}
