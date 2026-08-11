package com.coffeecommits.brakket.twitch.service;

import com.coffeecommits.brakket.twitch.event.MuestraChatCapturadaEvent;
import com.coffeecommits.brakket.twitch.model.MetricaChat;
import com.coffeecommits.brakket.twitch.model.PlataformaTransmision;
import com.coffeecommits.brakket.twitch.model.TransmisionTwitch;
import com.coffeecommits.brakket.twitch.repository.MetricaChatRepository;
import com.coffeecommits.brakket.twitch.repository.TransmisionTwitchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * RF-38: el muestreo del chat no debe inventar datos. Se cubren las tres
 * decisiones del tick — cuando no hay nada que capturar, cuando hay que
 * (re)conectar, y cuando corresponde guardar la muestra.
 */
@ExtendWith(MockitoExtension.class)
class MuestreoChatServiceTest {

    private static final long INTERVALO_MS = 20_000; // ventana de 20s: 1/3 de minuto

    @Mock
    private ChatTwitchListener listener;

    @Mock
    private TransmisionTwitchRepository transmisionRepository;

    @Mock
    private MetricaChatRepository metricaChatRepository;

    @Mock
    private ApplicationEventPublisher eventos;

    private MuestreoChatService service;

    @BeforeEach
    void setUp() {
        service = new MuestreoChatService(listener, transmisionRepository,
                metricaChatRepository, eventos, INTERVALO_MS);
    }

    /** La muestra persistida vuelve con id: el evento viaja por id, no por entidad. */
    private void alGuardarDevolverConId(long id) {
        when(metricaChatRepository.save(any(MetricaChat.class))).thenAnswer(invocacion -> {
            MetricaChat guardada = invocacion.getArgument(0);
            guardada.setId(id);
            return guardada;
        });
    }

    private TransmisionTwitch transmisionAbierta() {
        return TransmisionTwitch.builder()
                .id(7L)
                .loginCanal("brakketcenfotec")
                .plataforma(PlataformaTransmision.TWITCH)
                .estado("EN_VIVO")
                .build();
    }

    /**
     * RF-34: al finalizar la ultima transmision abierta el socket quedaba vivo
     * escuchando un canal que ya nadie mira.
     */
    @Test
    void al_cerrarse_la_ultima_transmision_se_suelta_la_conexion() {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));
        service.muestrear(); // conecta

        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of());
        service.muestrear();

        verify(listener).desconectar();
    }

    @Test
    void sin_transmision_abierta_no_conecta_ni_guarda() {
        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of());

        service.muestrear();

        verify(listener, never()).conectar(any());
        verify(metricaChatRepository, never()).save(any());
    }

    @Test
    void el_primer_tick_conecta_y_no_guarda_muestra() {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));

        service.muestrear();

        verify(listener).conectar("brakketcenfotec");
        // La ventana recien empieza: guardarla contaria menos de un intervalo.
        verify(metricaChatRepository, never()).save(any());
    }

    @Test
    void con_la_conexion_caida_reconecta_en_vez_de_guardar_ceros() {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));

        service.muestrear();              // conecta
        when(listener.estaConectado()).thenReturn(false); // se cayo entre ticks
        service.muestrear();              // debe reconectar, no guardar

        verify(listener, org.mockito.Mockito.times(2)).conectar("brakketcenfotec");
        verify(metricaChatRepository, never()).save(any());
    }

    @Test
    void guarda_la_tasa_por_minuto_no_el_conteo_de_la_ventana() {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));

        service.muestrear(); // primer tick: conecta

        alGuardarDevolverConId(500L);
        when(listener.estaConectado()).thenReturn(true);
        when(listener.tomarYReiniciar())
                .thenReturn(new ChatTwitchListener.Ventana(134, 122, List.of("hola", "gg")));

        service.muestrear(); // segundo tick: guarda

        ArgumentCaptor<MetricaChat> guardada = ArgumentCaptor.forClass(MetricaChat.class);
        verify(metricaChatRepository).save(guardada.capture());

        MetricaChat metrica = guardada.getValue();
        // 134 mensajes en 20s son 402 por minuto, no 134.
        assertThat(metrica.getMensajesPorMinuto()).isEqualTo(402);
        assertThat(metrica.getUsuariosActivos()).isEqualTo(122);
        assertThat(metrica.getTransmisionTwitch().getId()).isEqualTo(7L);
        // Las muestras cuelgan de la transmision, no del modelo por equipo de V1.
        assertThat(metrica.getCuentaTwitch()).isNull();
    }

    @Test
    void publica_la_ventana_para_el_analisis_de_sentimiento() {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));

        service.muestrear(); // primer tick: conecta

        alGuardarDevolverConId(500L);
        when(listener.estaConectado()).thenReturn(true);
        when(listener.tomarYReiniciar())
                .thenReturn(new ChatTwitchListener.Ventana(3, 2, List.of("gg", "que lag", "vamos")));

        service.muestrear();

        ArgumentCaptor<MuestraChatCapturadaEvent> publicado =
                ArgumentCaptor.forClass(MuestraChatCapturadaEvent.class);
        verify(eventos).publishEvent(publicado.capture());

        // El evento viaja con el id de la muestra recien guardada: el listener
        // corre en otra transaccion y necesita releerla, no la instancia.
        assertThat(publicado.getValue().metricaChatId()).isEqualTo(500L);
        assertThat(publicado.getValue().mensajes()).containsExactly("gg", "que lag", "vamos");
    }

    @Test
    void una_ventana_sin_texto_guarda_la_muestra_pero_no_publica_evento() {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));

        service.muestrear(); // primer tick: conecta

        alGuardarDevolverConId(501L);
        when(listener.estaConectado()).thenReturn(true);
        // Chat mudo: la tasa de cero es un dato valido, pero no hay nada que analizar.
        when(listener.tomarYReiniciar())
                .thenReturn(new ChatTwitchListener.Ventana(0, 0, List.of()));

        service.muestrear();

        verify(metricaChatRepository).save(any(MetricaChat.class));
        verify(eventos, never()).publishEvent(any(MuestraChatCapturadaEvent.class));
    }
}
