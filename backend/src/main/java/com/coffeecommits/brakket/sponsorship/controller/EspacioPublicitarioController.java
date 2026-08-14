package com.coffeecommits.brakket.sponsorship.controller;

import com.coffeecommits.brakket.sponsorship.dto.CrearEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EspacioPublicitarioResponse;
import com.coffeecommits.brakket.sponsorship.service.EspacioPublicitarioService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/espacios")
public class EspacioPublicitarioController {

    private final EspacioPublicitarioService espacioService;

    public EspacioPublicitarioController(EspacioPublicitarioService espacioService) {
        this.espacioService = espacioService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public EspacioPublicitarioResponse crear(@Valid @RequestBody CrearEspacioPublicitarioRequest request) {
        return espacioService.crear(request);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public EspacioPublicitarioResponse editar(@PathVariable Long id,
                                              @Valid @RequestBody EditarEspacioPublicitarioRequest request) {
        return espacioService.editar(id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAuthority('GESTIONAR_PATROCINIOS')")
    public void eliminar(@PathVariable Long id) {
        espacioService.eliminar(id);
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<EspacioPublicitarioResponse> listarPorPatrocinio(@RequestParam Long patrocinioId) {
        return espacioService.listarPorPatrocinio(patrocinioId);
    }

    @GetMapping("/vigente")
    public ResponseEntity<EspacioPublicitarioResponse> buscarVigente(
            @RequestParam(required = false) Long ligaId,
            @RequestParam(required = false) Long temporadaId,
            @RequestParam(required = false) Long torneoId,
            @RequestParam String ubicacion) {

        return espacioService.buscarVigente(ligaId, temporadaId, torneoId, ubicacion)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}