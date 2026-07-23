package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.tournament.repository.PartidaRepository;
import com.coffeecommits.brakket.tournament.repository.TorneoRepository;
import com.coffeecommits.brakket.twitch.dto.ConfigurarCanalTwitchRequest;
import com.coffeecommits.brakket.twitch.model.CanalOficialTwitch;
import com.coffeecommits.brakket.twitch.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import java.util.Optional;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CanalTwitchServiceTest {
    @Mock CanalOficialTwitchRepository canalRepository;
    @Mock TransmisionTwitchRepository transmisionRepository;
    @Mock IncidenteIntegracionTwitchRepository incidenteRepository;
    @Mock TorneoRepository torneoRepository;
    @Mock PartidaRepository partidaRepository;
    @Mock TwitchGateway gateway;
    TwitchProperties properties;
    CanalTwitchService service;

    @BeforeEach
    void setup() {
        properties = new TwitchProperties();
        service = new CanalTwitchService(canalRepository, transmisionRepository, incidenteRepository,
                torneoRepository, partidaRepository, gateway, properties);
        when(canalRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());
        when(canalRepository.save(any())).thenAnswer(invocation -> {
            CanalOficialTwitch c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });
    }

    @Test
    void guardaPendienteCuandoNoHayCredenciales() {
        var response = service.configurar(
                new ConfigurarCanalTwitchRequest("https://www.twitch.tv/brakketcenfotec"));

        assertThat(response.loginCanal()).isEqualTo("brakketcenfotec");
        assertThat(response.estado()).isEqualTo("PENDIENTE");
        assertThat(response.activo()).isFalse();
        verifyNoInteractions(gateway);
    }

    @Test
    void validaYActivaCanalExistente() {
        properties.setClientId("client");
        properties.setClientSecret("secret");
        when(gateway.findChannel("brakketcenfotec"))
                .thenReturn(new TwitchGateway.ChannelInfo("123", "brakketcenfotec", "BrakketCenfotec"));

        var response = service.configurar(new ConfigurarCanalTwitchRequest("brakketcenfotec"));

        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.twitchUsuarioId()).isEqualTo("123");
        assertThat(response.activo()).isTrue();
    }
}

