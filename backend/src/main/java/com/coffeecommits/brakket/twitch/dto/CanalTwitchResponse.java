package com.coffeecommits.brakket.twitch.dto;
import java.time.LocalDateTime;
public record CanalTwitchResponse(Long id, String twitchUsuarioId, String loginCanal,
    String nombreMostrado, String urlCanal, String estado, boolean activo,
    String ultimoError, LocalDateTime ultimaValidacion, boolean credencialesConfiguradas) {}

