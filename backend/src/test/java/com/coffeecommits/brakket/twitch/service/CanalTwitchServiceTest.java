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
    @Mock MetricaAudienciaRepository metricaRepository;
    @Mock TorneoRepository torneoRepository;
    @Mock PartidaRepository partidaRepository;
    @Mock TwitchGateway gateway;
    TwitchProperties properties;
    CanalTwitchService service;

    @BeforeEach
    void setup() {
        properties = new TwitchProperties();
        service = new CanalTwitchService(canalRepository, transmisionRepository, incidenteRepository,
                metricaRepository, torneoRepository, partidaRepository, gateway, properties);
        // lenient: los tests de finalizar no tocan el canal, y el modo estricto
        // marcaria estos stubs compartidos como innecesarios.
        lenient().when(canalRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());
        lenient().when(canalRepository.save(any())).thenAnswer(invocation -> {
            CanalOficialTwitch c = invocation.getArgument(0);
            c.setId(1L);
            return c;
        });
    }

    @Test
    void finalizarCierraElPeriodoDeCaptura() {
        var transmision = com.coffeecommits.brakket.twitch.model.TransmisionTwitch.builder()
                .id(7L).estado("EN_VIVO")
                .iniciadaEn(java.time.LocalDateTime.of(2026, 7, 24, 18, 0))
                .creadaEn(java.time.LocalDateTime.of(2026, 7, 24, 18, 0)).build();
        when(transmisionRepository.findById(7L)).thenReturn(Optional.of(transmision));

        var response = service.finalizar(7L);

        assertThat(response.estado()).isEqualTo("FINALIZADA");
        assertThat(transmision.getFinalizadaEn()).isNotNull();
        verify(transmisionRepository).save(transmision);
    }

    @Test
    void finalizarDosVecesFalla() {
        var transmision = com.coffeecommits.brakket.twitch.model.TransmisionTwitch.builder()
                .id(7L).estado("FINALIZADA")
                .finalizadaEn(java.time.LocalDateTime.of(2026, 7, 24, 19, 0))
                .creadaEn(java.time.LocalDateTime.of(2026, 7, 24, 18, 0)).build();
        when(transmisionRepository.findById(7L)).thenReturn(Optional.of(transmision));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.finalizar(7L))
                .isInstanceOf(com.coffeecommits.brakket.common.exception.BusinessException.class);
        verify(transmisionRepository, never()).save(any());
    }

    /** RF-34: reasociar el mismo directo con otra abierta debe explicarse, no reventar. */
    @Test
    void noSePuedeAsociarDosVecesElMismoDirectoAbierto() {
        var canal = CanalOficialTwitch.builder().id(1L).loginCanal("sol1xd").activo(true).build();
        when(canalRepository.findFirstByActivoTrue()).thenReturn(Optional.of(canal));
        when(torneoRepository.findById(27L)).thenReturn(Optional.of(
                com.coffeecommits.brakket.tournament.model.Torneo.builder().id(27L).build()));
        when(gateway.findLiveStream("sol1xd")).thenReturn(
                new TwitchGateway.StreamInfo("317466024803", 4300, java.time.LocalDateTime.now()));
        when(transmisionRepository.findByTwitchStreamIdAndFinalizadaEnIsNull("317466024803"))
                .thenReturn(Optional.of(com.coffeecommits.brakket.twitch.model.TransmisionTwitch.builder()
                        .id(7L).build()));

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.asociar(
                        new com.coffeecommits.brakket.twitch.dto.AsociarTransmisionRequest(27L, null)))
                .isInstanceOf(com.coffeecommits.brakket.common.exception.BusinessException.class)
                .hasMessageContaining("Finalizala");
        verify(transmisionRepository, never()).save(any());
    }

    @Test
    void finalizarUnaTransmisionInexistenteDaNotFound() {
        when(transmisionRepository.findById(99L)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> service.finalizar(99L))
                .isInstanceOf(com.coffeecommits.brakket.common.exception.ResourceNotFoundException.class);
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

