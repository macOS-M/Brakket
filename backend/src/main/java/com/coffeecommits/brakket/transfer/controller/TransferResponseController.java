package com.coffeecommits.brakket.transfer.controller;

import com.coffeecommits.brakket.transfer.dto.ResponderTransferenciaRequest;
import com.coffeecommits.brakket.transfer.dto.TransferenciaResponse;
import com.coffeecommits.brakket.transfer.service.TransferResponseService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Respuesta a solicitudes de transferencia (RF-13, EPIC-03). El jugador y el
 * capitán del equipo origen aceptan o rechazan; requiere JWT válido.
 */
@RestController
@RequestMapping("/api/transfers")
public class TransferResponseController {

    private final TransferResponseService transferResponseService;

    public TransferResponseController(TransferResponseService transferResponseService) {
        this.transferResponseService = transferResponseService;
    }

    /** Bandeja de solicitudes pendientes donde el usuario debe responder. */
    @GetMapping("/pendientes")
    public List<TransferenciaResponse> pendientes(Authentication authentication) {
        return transferResponseService.listarPendientes(authentication.getName());
    }

    /** Registra la aceptación o el rechazo de una parte autorizada. */
    @PostMapping("/{id}/responder")
    public TransferenciaResponse responder(@PathVariable Long id,
                                           @Valid @RequestBody ResponderTransferenciaRequest request,
                                           Authentication authentication) {
        return transferResponseService.responder(authentication.getName(), id, request);
    }
}
