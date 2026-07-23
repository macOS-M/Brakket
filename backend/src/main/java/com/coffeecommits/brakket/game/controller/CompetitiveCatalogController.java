package com.coffeecommits.brakket.game.controller;

import com.coffeecommits.brakket.game.dto.CatalogoCompetitivoResponse;
import com.coffeecommits.brakket.game.repository.EstadisticaJuegoRepository;
import com.coffeecommits.brakket.game.repository.FormatoCompetitivoRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/competitive-catalogs")
public class CompetitiveCatalogController {

    private final FormatoCompetitivoRepository formatoRepository;
    private final EstadisticaJuegoRepository estadisticaRepository;

    public CompetitiveCatalogController(FormatoCompetitivoRepository formatoRepository,
                                        EstadisticaJuegoRepository estadisticaRepository) {
        this.formatoRepository = formatoRepository;
        this.estadisticaRepository = estadisticaRepository;
    }

    @GetMapping("/formats")
    public List<CatalogoCompetitivoResponse> formatos() {
        return formatoRepository.findByActivoTrueOrderByNombreAsc().stream()
                .map(f -> new CatalogoCompetitivoResponse(f.getId(), f.getNombre(), false))
                .toList();
    }

    @GetMapping("/statistics")
    public List<CatalogoCompetitivoResponse> estadisticas() {
        return estadisticaRepository.findByActivaTrueOrderByNombreAsc().stream()
                .map(e -> new CatalogoCompetitivoResponse(e.getId(), e.getNombre(), e.getObligatoria()))
                .toList();
    }
}
