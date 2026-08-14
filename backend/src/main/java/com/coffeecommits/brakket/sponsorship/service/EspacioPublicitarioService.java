package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.sponsorship.dto.CrearEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarEspacioPublicitarioRequest;
import com.coffeecommits.brakket.sponsorship.dto.EspacioPublicitarioResponse;

import java.util.List;
import java.util.Optional;

public interface EspacioPublicitarioService {

    EspacioPublicitarioResponse crear(CrearEspacioPublicitarioRequest request);

    EspacioPublicitarioResponse editar(Long id, EditarEspacioPublicitarioRequest request);

    void eliminar(Long id);

    List<EspacioPublicitarioResponse> listarPorPatrocinio(Long patrocinioId);

    Optional<EspacioPublicitarioResponse> buscarVigente(Long ligaId, Long temporadaId, Long torneoId,
                                                        String ubicacion);
}