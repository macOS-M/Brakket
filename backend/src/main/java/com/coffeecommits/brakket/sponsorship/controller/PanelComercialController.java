package com.coffeecommits.brakket.sponsorship.controller;

import com.coffeecommits.brakket.sponsorship.dto.MetricasPatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.dto.PanelComercialResponse;
import com.coffeecommits.brakket.sponsorship.service.PanelComercialService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sponsors/me/panel")
public class PanelComercialController {

    private final PanelComercialService panelService;

    public PanelComercialController(PanelComercialService panelService) {
        this.panelService = panelService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('VER_METRICAS_AUDIENCIA')")
    public PanelComercialResponse obtenerResumen(Authentication authentication) {
        return panelService.obtenerResumen(authentication);
    }

    @GetMapping("/metricas")
    @PreAuthorize("hasAuthority('VER_METRICAS_AUDIENCIA')")
    public MetricasPatrocinioResponse obtenerMetricas(Authentication authentication,
                                                      @RequestParam Long patrocinioId) {
        return panelService.obtenerMetricas(authentication, patrocinioId);
    }
}