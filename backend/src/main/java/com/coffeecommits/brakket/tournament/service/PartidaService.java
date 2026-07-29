package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.tournament.dto.PartidaResponse;
import com.coffeecommits.brakket.tournament.dto.RegistrarCasoEspecialRequest;
import com.coffeecommits.brakket.tournament.dto.ReportarResultadoRequest;

import java.util.List;

public interface PartidaService {

    List<PartidaResponse> iniciarTorneo(Long torneoId, String correo, boolean esAdmin);

    List<PartidaResponse> obtenerBracket(Long torneoId, String correoOpcional, boolean esAdmin);

    PartidaResponse reportar(Long partidaId, String correo, ReportarResultadoRequest request);

    PartidaResponse confirmar(Long partidaId, String correo);

    PartidaResponse rechazar(Long partidaId, String correo);

    PartidaResponse resolver(Long partidaId, String correo, boolean esAdmin,
                             ReportarResultadoRequest request);

    PartidaResponse registrarCasoEspecial(Long partidaId, String correo, boolean esAdmin,
                                          RegistrarCasoEspecialRequest request);
}