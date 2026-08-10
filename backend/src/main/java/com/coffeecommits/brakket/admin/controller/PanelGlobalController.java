package com.coffeecommits.brakket.admin.controller;

import com.coffeecommits.brakket.admin.dto.LogAuditoriaResponse;
import com.coffeecommits.brakket.admin.dto.PanelGlobalResponse;
import com.coffeecommits.brakket.admin.service.AdminPanelService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Panel global de administración (RF-49, EPIC-14). Solo ADMIN: es la vista de
 * supervisión de toda la plataforma. Complementa la gestión de roles (RF-19),
 * que vive en {@link RolController} bajo el mismo prefijo {@code /api/admin}.
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class PanelGlobalController {

    private final AdminPanelService adminPanelService;

    /** Tablero: conteos globales + actividad de auditoría reciente. */
    @GetMapping("/panel")
    @PreAuthorize("hasRole('ADMIN')")
    public PanelGlobalResponse panel() {
        return adminPanelService.panel();
    }

    /** Listado de auditoría reciente (por defecto 50 entradas, tope 100). */
    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public List<LogAuditoriaResponse> auditoria(
            @RequestParam(defaultValue = "50") int limite) {
        return adminPanelService.auditoriaReciente(limite);
    }
}
