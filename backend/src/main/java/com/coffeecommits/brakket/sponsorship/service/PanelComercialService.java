package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.sponsorship.dto.MetricasPatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.dto.PanelComercialResponse;
import org.springframework.security.core.Authentication;

public interface PanelComercialService {

    PanelComercialResponse obtenerResumen(Authentication authentication);

    MetricasPatrocinioResponse obtenerMetricas(Authentication authentication, Long patrocinioId);
}