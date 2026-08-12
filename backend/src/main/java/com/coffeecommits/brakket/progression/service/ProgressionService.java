package com.coffeecommits.brakket.progression.service;
import com.coffeecommits.brakket.progression.dto.ProgresionResponse;
import com.coffeecommits.brakket.progression.dto.PerfilPersonalizadoResponse;

public interface ProgressionService {
    ProgresionResponse consultar(String correo);
    ProgresionResponse canjear(String correo, Long elementoId);
    ProgresionResponse aplicar(String correo, Long elementoId);
    ProgresionResponse quitar(String correo, Long elementoId);
    void registrarLogro(Long usuarioId, Long logroId, String referenciaSistema);
    void revertirLogro(Long usuarioId, Long logroId);
    PerfilPersonalizadoResponse perfilPublico(Long usuarioId);
}
