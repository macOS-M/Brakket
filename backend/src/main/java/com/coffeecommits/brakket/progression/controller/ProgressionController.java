package com.coffeecommits.brakket.progression.controller;
import com.coffeecommits.brakket.progression.dto.ProgresionResponse;
import com.coffeecommits.brakket.progression.service.ProgressionService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/progression")
public class ProgressionController {
    private final ProgressionService service;
    public ProgressionController(ProgressionService service) { this.service=service; }
    @GetMapping public ProgresionResponse consultar(Authentication auth) { return service.consultar(auth.getName()); }
    @PostMapping("/redeem/{id}") public ProgresionResponse canjear(@PathVariable Long id, Authentication auth) { return service.canjear(auth.getName(),id); }
    @PutMapping("/apply/{id}") public ProgresionResponse aplicar(@PathVariable Long id, Authentication auth) { return service.aplicar(auth.getName(),id); }
    @DeleteMapping("/apply/{id}") public ProgresionResponse quitar(@PathVariable Long id, Authentication auth) { return service.quitar(auth.getName(),id); }
}
