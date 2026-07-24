package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.config.TwitchProperties;
import com.coffeecommits.brakket.transmision.service.StreamProvider;
import com.coffeecommits.brakket.twitch.model.MetricaAudiencia;
import com.coffeecommits.brakket.twitch.model.OrigenMetrica;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.IncidenteIntegracionTwitchRepository;
import com.coffeecommits.brakket.twitch.repository.MetricaAudienciaRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MuestreoAudienciaServiceTest {
    @Mock TransmisionTwitchRepository transmisionRepository;
    @Mock MetricaAudienciaRepository metricaRepository;
    @Mock IncidenteIntegracionTwitchRepository incidenteRepository;
    @Mock StreamProvider provider;

    TwitchProperties properties;
    MuestreoAudienciaService service;

    private static final StreamProvider.StreamEnVivo DIRECTO = new StreamProvider.StreamEnVivo(
            "999", "123", "brakketcenfotec", "Gran final", 57, "https://cdn/t.jpg",
            "League of Legends", "es", LocalDateTime.of(2026, 7, 24, 18, 0));

    @BeforeEach
    void setup() {
        properties = new TwitchProperties();
        properties.setClientId("client");
        properties.setClientSecret("secret");
        when(provider.plataforma())
                .thenReturn(com.coffeecommits.brakket.twitch.model.PlataformaTransmision.TWITCH);
        service = new MuestreoAudienciaService(transmisionRepository, metricaRepository,
                incidenteRepository, properties, List.of(provider));
    }

    private TransmisionTwitch abierta() {
        return TransmisionTwitch.builder().id(7L).loginCanal("brakketcenfotec")
                .estado("SIN_DATOS_EN_VIVO").creadaEn(LocalDateTime.now()).build();
    }

    @Test
    void guardaUnaMuestraRealCuandoLaTransmisionEstaEnVivo() {
        TransmisionTwitch transmision = abierta();
        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of(transmision));
        when(provider.getLiveStreams(List.of("brakketcenfotec"))).thenReturn(List.of(DIRECTO));

        service.muestrear();

        ArgumentCaptor<MetricaAudiencia> captor = ArgumentCaptor.forClass(MetricaAudiencia.class);
        verify(metricaRepository).save(captor.capture());
        MetricaAudiencia muestra = captor.getValue();
        assertThat(muestra.getEspectadores()).isEqualTo(57);
        assertThat(muestra.getOrigen()).isEqualTo(OrigenMetrica.REAL);
        assertThat(muestra.getTransmisionTwitch()).isSameAs(transmision);
        assertThat(muestra.getFechaHora()).isNotNull();
        // La transmisión queda verificada y con los datos del directo.
        assertThat(transmision.getEstado()).isEqualTo("EN_VIVO");
        assertThat(transmision.getTwitchStreamId()).isEqualTo("999");
        assertThat(transmision.getIniciadaEn()).isEqualTo(DIRECTO.iniciadoEn());
    }

    @Test
    void cierraElPeriodoCuandoElDirectoTermina() {
        TransmisionTwitch transmision = abierta();
        transmision.setEstado("EN_VIVO");
        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of(transmision));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of());

        service.muestrear();

        assertThat(transmision.getEstado()).isEqualTo("FINALIZADA");
        assertThat(transmision.getFinalizadaEn()).isNotNull();
        verify(metricaRepository, never()).save(any());
    }

    @Test
    void noInventaDatosSiTwitchNoResponde() {
        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of(abierta()));
        when(provider.getLiveStreams(anyList()))
                .thenThrow(new TwitchUnavailableException("caído", null));

        service.muestrear();
        service.muestrear(); // segundo tick dentro del throttle

        verify(metricaRepository, never()).save(any());
        // El incidente se registra UNA vez (throttle), no una por tick.
        verify(incidenteRepository).save(any());
    }

    @Test
    void noHaceNadaSinCredenciales() {
        properties.setClientId(null);

        service.muestrear();

        verifyNoInteractions(transmisionRepository, metricaRepository, incidenteRepository);
    }

    @Test
    void unaTransmisionAunNoIniciadaSigueAbiertaSinMuestras() {
        TransmisionTwitch transmision = abierta(); // SIN_DATOS_EN_VIVO, nunca estuvo en vivo
        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of(transmision));
        when(provider.getLiveStreams(anyList())).thenReturn(List.of());

        service.muestrear();

        // No estaba en vivo: no hay nada que cerrar ni que guardar.
        assertThat(transmision.getEstado()).isEqualTo("SIN_DATOS_EN_VIVO");
        assertThat(transmision.getFinalizadaEn()).isNull();
        verify(metricaRepository, never()).save(any());
    }
}
