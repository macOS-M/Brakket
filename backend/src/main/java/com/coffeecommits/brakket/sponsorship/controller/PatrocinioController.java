package com.coffeecommits.brakket.sponsorship.controller;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinioRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinioResponse;
import com.coffeecommits.brakket.sponsorship.service.PatrocinioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/patrocinios")
public class PatrocinioController {

    private final PatrocinioService patrocinioService;

    public PatrocinioController(PatrocinioService patrocinioService) {
        this.patrocinioService = patrocinioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public PatrocinioResponse crear(@Valid @RequestBody CrearPatrocinioRequest request) {
        return patrocinioService.crear(request);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PatrocinioResponse obtener(@PathVariable Long id) {
        return patrocinioService.obtenerPorId(id);
    }

    // Un solo endpoint de listado con filtro por alcance, consistente con como
    // el frontend consultara el componente <app-ad-slot> mas adelante en RF-43.
    // Solo uno de los tres parametros debe llegar a la vez.
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PatrocinioResponse> listar(@RequestParam(required = false) Long torneoId,
                                           @RequestParam(required = false) Long ligaId,
                                           @RequestParam(required = false) Long temporadaId) {
        if (torneoId != null) {
            return patrocinioService.listarPorTorneo(torneoId);
        }
        if (ligaId != null) {
            return patrocinioService.listarPorLiga(ligaId);
        }
        if (temporadaId != null) {
            return patrocinioService.listarPorTemporada(temporadaId);
        }
        return patrocinioService.listarTodos();
    }
}