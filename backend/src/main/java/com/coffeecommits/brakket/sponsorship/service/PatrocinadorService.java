package com.coffeecommits.brakket.sponsorship.service;

import com.coffeecommits.brakket.sponsorship.dto.CambiarEstadoPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.CrearPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.EditarPatrocinadorRequest;
import com.coffeecommits.brakket.sponsorship.dto.PatrocinadorResponse;

import java.util.List;

public interface PatrocinadorService {

    PatrocinadorResponse crear(CrearPatrocinadorRequest request);

    PatrocinadorResponse editar(Long id, EditarPatrocinadorRequest request);

    PatrocinadorResponse cambiarEstado(Long id, CambiarEstadoPatrocinadorRequest request);

    PatrocinadorResponse obtenerPorId(Long id);

    List<PatrocinadorResponse> listar();
}