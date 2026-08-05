package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.tournament.dto.PartidaResponse;
import com.coffeecommits.brakket.tournament.dto.RegistrarCasoEspecialRequest;
import com.coffeecommits.brakket.tournament.dto.ReportarResultadoRequest;

import java.util.List;

/**
 * Motor de competencias mínimo (RF-26/27/29): bracket de eliminación
 * directa, lobby de partida privada por enfrentamiento y resultados
 * reporta-el-capitán / confirma-el-rival, con el organizador (o un ADMIN)
 * como desempate.
 */
public interface PartidaService {

    /**
     * Cierra inscripciones, genera el bracket y pone el torneo EN_CURSO.
     * Solo el organizador o un ADMIN; exige al menos 2 equipos inscritos.
     */
    List<PartidaResponse> iniciarTorneo(Long torneoId, String correo, boolean esAdmin);

    /** El bracket completo; mismas reglas de visibilidad que el torneo. */
    List<PartidaResponse> obtenerBracket(Long torneoId, String correoOpcional, boolean esAdmin);

    /** Un capitán de la partida reporta el marcador. */
    PartidaResponse reportar(Long partidaId, String correo, ReportarResultadoRequest request);

    /** El capitán rival confirma el reporte; el ganador avanza en la llave. */
    PartidaResponse confirmar(Long partidaId, String correo);

    /** El capitán rival rechaza el reporte: la partida queda en disputa. */
    PartidaResponse rechazar(Long partidaId, String correo);

    /** El organizador (o un ADMIN) fija el resultado final de la partida. */
    PartidaResponse resolver(Long partidaId, String correo, boolean esAdmin,
                             ReportarResultadoRequest request);

    PartidaResponse registrarCasoEspecial(Long partidaId, String correo, boolean esAdmin,
                                          RegistrarCasoEspecialRequest request);

    /** RF-28: historial de descansos/avances/abandonos de una partida. */
    java.util.List<com.coffeecommits.brakket.tournament.dto.CasoEspecialResponse> historialCasoEspecial(
            Long partidaId, String correo, boolean esAdmin);

    /**
     * RF-32: tras resolver una disputa, vuelve a fijar el resultado.
     * equipoGanadorId null = mantener el resultado original; con un ID,
     * lo revierte a favor de ese equipo (bloqueado si la llave ya avanzó
     * más allá de este cruce).
     */
    PartidaResponse finalizarPorResolucionDeDisputa(Long partidaId, Long equipoGanadorId);
}