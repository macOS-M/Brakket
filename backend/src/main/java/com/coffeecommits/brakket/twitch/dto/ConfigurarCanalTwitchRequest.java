package com.coffeecommits.brakket.twitch.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ConfigurarCanalTwitchRequest(
    @NotBlank(message = "El canal es obligatorio")
    @Pattern(regexp = "^(?:https?://)?(?:www\\.)?twitch\\.tv/[A-Za-z0-9_]{4,25}/?$|^[A-Za-z0-9_]{4,25}$",
             message = "Ingrese un usuario o URL válida de Twitch")
    String canal
) {}

