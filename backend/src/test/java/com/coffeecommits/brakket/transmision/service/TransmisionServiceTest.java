package com.coffeecommits.brakket.transmision.service;

import com.coffeecommits.brakket.config.StreamsProperties;
import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.transmision.dto.TransmisionesResponse;
import com.coffeecommits.brakket.twitch.model.CanalOficialTwitch;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.CanalOficialTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.IncidenteIntegracionTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import com.coffeecommits.brakket.twitch.service.TwitchUnavailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TransmisionServiceTest {
    @Mock CanalOficialTwitchRepository canalRepository;
    @Mock TransmisionTwitchRepository transmisionRepository;
    @Mock IncidenteIntegracionTwitchRepository incidenteRepository;
    @Mock StreamProvider provider;

    TwitchProperties twitchProperties;
    StreamsProperties streamsProperties;
    TransmisionService service;

    private static final StreamProvider.CanalStream CANAL_BRAKKET = new StreamProvider.CanalStream(
            "123", "brakketcenfotec", "BrakketCenfotec", "https://cdn/avatar.png", "https://cdn/offline.png");
    private static final StreamProvider.StreamEnVivo DIRECTO_BRAKKET = new StreamProvider.StreamEnVivo(
            "999", "123", "brakketcenfotec", "Gran final", 42, "https://cdn/thumb.jpg",
            "League of Legends", "es", LocalDateTime.of(2026, 7, 23, 18, 0));

    @BeforeEach
    void setup() {
        twitchProperties = new TwitchProperties();
        twitchProperties.setChannel("brakketcenfotec");
        streamsProperties = new StreamsProperties();
        when(provider.plataforma()).thenReturn(PlataformaTransmision.TWITCH);
        when(canalRepository.findFirstByActivoTrue()).thenReturn(Optional.empty());
        service = new TransmisionService(canalRepository, transmisionRepository, incidenteRepository,
                twitchProperties, streamsProperties, List.of(provider));
    }

    @Test
    void muestraElCanalOficialEnVivo() {
        when(provider.getChannels(anyList())).thenReturn(List.of(CANAL_BRAKKET));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of(DIRECTO_BRAKKET));

        TransmisionesResponse respuesta = service.listar();

        assertThat(respuesta.degradado()).isFalse();
        assertThat(respuesta.transmisiones()).hasSize(1);
        var t = respuesta.transmisiones().get(0);
        assertThat(t.estado()).isEqualTo("EN_VIVO");
        assertThat(t.destacada()).isTrue();
        assertThat(t.titulo()).isEqualTo("Gran final");
        assertThat(t.espectadores()).isEqualTo(42);
        assertThat(t.categoria()).isEqualTo("League of Legends");
        assertThat(t.vod()).isNull();
    }

    @Test
    void elCanalDeBdTienePrioridadSobreLaEnvVar() {
        when(canalRepository.findFirstByActivoTrue()).thenReturn(Optional.of(
                CanalOficialTwitch.builder().loginCanal("canaldb").build()));
        when(provider.getChannels(List.of("canaldb"))).thenReturn(List.of());

        service.listar();

        verify(provider).getChannels(List.of("canaldb"));
    }

    @Test
    void cacheaLaRespuestaDentroDelTtl() {
        when(provider.getChannels(anyList())).thenReturn(List.of(CANAL_BRAKKET));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of(DIRECTO_BRAKKET));

        service.listar();
        service.listar();

        verify(provider, times(1)).getChannels(anyList());
    }

    @Test
    void canalOfflineIncluyeSuUltimoVod() {
        when(provider.getChannels(anyList())).thenReturn(List.of(CANAL_BRAKKET));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of());
        when(provider.getLatestVod("123")).thenReturn(new StreamProvider.VodInfo(
                "v1", "https://www.twitch.tv/videos/v1", "VOD final", "https://cdn/vod.jpg",
                "2h10m", LocalDateTime.of(2026, 7, 20, 20, 0)));

        var t = service.listar().transmisiones().get(0);

        assertThat(t.estado()).isEqualTo("OFFLINE");
        assertThat(t.espectadores()).isNull();
        assertThat(t.vod()).isNotNull();
        assertThat(t.vod().titulo()).isEqualTo("VOD final");
        // Sin directo, el "thumbnail" de la tarjeta es la imagen offline del canal.
        assertThat(t.thumbnailUrl()).isEqualTo("https://cdn/offline.png");
    }

    @Test
    void unVodQueFallaNoImpideLaTarjetaOffline() {
        when(provider.getChannels(anyList())).thenReturn(List.of(CANAL_BRAKKET));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of());
        when(provider.getLatestVod("123")).thenThrow(new TwitchUnavailableException("videos caído", null));

        TransmisionesResponse respuesta = service.listar();

        assertThat(respuesta.degradado()).isFalse();
        var t = respuesta.transmisiones().get(0);
        assertThat(t.estado()).isEqualTo("OFFLINE");
        assertThat(t.vod()).isNull();
    }

    @Test
    void conTwitchCaidoDegradaAlUltimoConocidoComoDesconocido() {
        streamsProperties.setCacheTtlSegundos(0);
        when(provider.getChannels(anyList())).thenReturn(List.of(CANAL_BRAKKET));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of(DIRECTO_BRAKKET));
        TransmisionesResponse buena = service.listar();

        when(provider.getChannels(anyList())).thenThrow(new TwitchUnavailableException("caído", null));
        TransmisionesResponse degradada = service.listar();

        assertThat(degradada.degradado()).isTrue();
        var t = degradada.transmisiones().get(0);
        assertThat(t.estado()).isEqualTo("DESCONOCIDO");
        // Nunca un contador viejo como si fuera actual.
        assertThat(t.espectadores()).isNull();
        // actualizadoEn conserva la hora de la última respuesta BUENA para que
        // la UI pueda decir "última información de hace X minutos".
        assertThat(degradada.actualizadoEn()).isEqualTo(buena.actualizadoEn());
        verify(incidenteRepository).save(any());
    }

    @Test
    void sinRespuestaBuenaPreviaDegradaConDatosDeBd() {
        when(provider.getChannels(anyList())).thenThrow(new TwitchUnavailableException("caído", null));

        TransmisionesResponse respuesta = service.listar();

        assertThat(respuesta.degradado()).isTrue();
        assertThat(respuesta.actualizadoEn()).isNull();
        var t = respuesta.transmisiones().get(0);
        assertThat(t.estado()).isEqualTo("DESCONOCIDO");
        assertThat(t.loginCanal()).isEqualTo("brakketcenfotec");
        assertThat(t.urlCanal()).isEqualTo("https://www.twitch.tv/brakketcenfotec");
    }

    @Test
    void conElFlagApagadoNoConsultaLasTransmisionesRegistradas() {
        when(provider.getChannels(anyList())).thenReturn(List.of(CANAL_BRAKKET));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of());

        service.listar();

        verifyNoInteractions(transmisionRepository);
    }

    @Test
    void conElFlagEncendidoIncorporaLasRegistradasSinDuplicar() {
        streamsProperties.setMultiSourceEnabled(true);
        when(transmisionRepository.findActivasConCanalYTorneo()).thenReturn(List.of(
                TransmisionTwitch.builder().loginCanal("otrocanal").build(),
                // Duplicada con el canal oficial: no debe consultarse dos veces.
                TransmisionTwitch.builder().loginCanal("brakketcenfotec").build()));
        when(provider.getChannels(anyList())).thenReturn(List.of());

        service.listar();

        verify(provider).getChannels(List.of("brakketcenfotec", "otrocanal"));
    }
}
