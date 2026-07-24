package com.coffeecommits.brakket.sponsorship.controller;

import com.coffeecommits.brakket.sponsorship.dto.CambiarEstadoPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinadorResponse;
import com.coffeecommits.brakket.sponsorship.service.PatrocinadorService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sponsors")
public class PatrocinadorController {

    private final PatrocinadorService patrocinadorService;

    public PatrocinadorController(PatrocinadorService patrocinadorService) {
        this.patrocinadorService = patrocinadorService;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<PatrocinadorResponse> listar() {
        return patrocinadorService.listar();
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public PatrocinadorResponse obtener(@PathVariable Long id) {
        return patrocinadorService.obtenerPorId(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public PatrocinadorResponse crear(@Valid @RequestBody CrearPatrocinadorRequest request) {
        return patrocinadorService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public PatrocinadorResponse editar(@PathVariable Long id, @Valid @RequestBody EditarPatrocinadorRequest request) {
        return patrocinadorService.editar(id, request);
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public PatrocinadorResponse cambiarEstado(@PathVariable Long id,
                                              @Valid @RequestBody CambiarEstadoPatrocinadorRequest request) {
        return patrocinadorService.cambiarEstado(id, request);
    }
}