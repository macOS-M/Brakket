package com.coffeecommits.brakket.twitch.dto;
import java.time.LocalDateTime;
public record TransmisionTwitchResponse(Long id, String twitchStreamId, Long torneoId,
    Long partidaId, String estado, LocalDateTime iniciadaEn) {}

