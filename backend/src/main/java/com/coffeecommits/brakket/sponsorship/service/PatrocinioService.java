package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinioRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinioResponse;

import java.util.List;

public interface PatrocinioService {

    PatrocinioResponse crear(CrearPatrocinioRequest request);

    PatrocinioResponse obtenerPorId(Long id);

    List<PatrocinioResponse> listarPorTorneo(Long torneoId);

    List<PatrocinioResponse> listarPorLiga(Long ligaId);

    List<PatrocinioResponse> listarPorTemporada(Long temporadaId);

    List<PatrocinioResponse> listarTodos();
}