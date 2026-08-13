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
    private com.coffeecommits.brakket.twitch.repository.MensajeChatRepository mensajeChatRepository;

    @Mock
    private ApplicationEventPublisher eventos;

    private MuestreoChatService service;

    /** Hora de referencia de los mensajes de prueba; el valor exacto da igual. */
    private static final java.time.LocalDateTime BASE = java.time.LocalDateTime.of(2026, 8, 13, 10, 0);

    /**
     * Ventana de prueba. El conteo va aparte de los textos porque el muestreo
     * puede haber contado más mensajes de los que conserva.
     */
    private ChatTwitchListener.Ventana ventana(int mensajes, int autores, String... textos) {
        return new ChatTwitchListener.Ventana(mensajes, autores,
                java.util.Arrays.stream(textos)
                        .map(texto -> new ChatTwitchListener.MensajeCapturado(texto, BASE))
                        .toList());
    }

    @BeforeEach
    void setUp() {
        // Bloque de cero minutos: cada ventana cierra su bloque, que es el
        // comportamiento que ejercen las pruebas del tick. El agrupamiento tiene
        // sus propias pruebas mas abajo.
        service = servicio(0, 180);
    }

    private MuestreoChatService servicio(long bloqueMinutos, int maxMensajesBloque) {
        return new MuestreoChatService(listener, transmisionRepository, metricaChatRepository,
                mensajeChatRepository, eventos, INTERVALO_MS, bloqueMinutos, maxMensajesBloque);
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
                .thenReturn(ventana(134, 122, "hola", "gg"));

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
                .thenReturn(ventana(3, 2, "gg", "que lag", "vamos"));

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
                .thenReturn(ventana(0, 0));

        service.muestrear();

        verify(metricaChatRepository).save(any(MetricaChat.class));
        verify(eventos, never()).publishEvent(any(MuestraChatCapturadaEvent.class));
    }

    // --- contenido de los mensajes (RF-38) -----------------------------------

    @Test
    void guarda_el_contenido_de_cada_mensaje_con_su_hora() {
        conectar(service);
        alGuardarDevolverConId(700L);
        when(listener.tomarYReiniciar()).thenReturn(ventana(2, 2, "gg", "que lag"));

        service.muestrear();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.coffeecommits.brakket.twitch.model.MensajeChat>> guardados =
                ArgumentCaptor.forClass(List.class);
        verify(mensajeChatRepository).saveAll(guardados.capture());

        assertThat(guardados.getValue())
                .extracting(com.coffeecommits.brakket.twitch.model.MensajeChat::getTexto)
                .containsExactly("gg", "que lag");
        assertThat(guardados.getValue()).allSatisfy(mensaje -> {
            // La hora es la de llegada del mensaje, no la del cierre de la ventana.
            assertThat(mensaje.getFechaHora()).isEqualTo(BASE);
            assertThat(mensaje.getTransmisionTwitch().getId()).isEqualTo(7L);
        });
    }

    @Test
    void una_ventana_muda_no_guarda_mensajes() {
        conectar(service);
        alGuardarDevolverConId(701L);
        when(listener.tomarYReiniciar()).thenReturn(ventana(0, 0));

        service.muestrear();

        // La tasa de cero si es un dato valido y se guarda; mensajes no hay.
        verify(metricaChatRepository).save(any(MetricaChat.class));
        verify(mensajeChatRepository, never()).saveAll(any());
    }

    // --- agrupamiento del analisis de sentimiento ----------------------------

    /** Conecta y deja el servicio listo para que el proximo tick guarde. */
    private void conectar(MuestreoChatService servicio) {
        when(transmisionRepository.findAbiertasParaMuestreo())
                .thenReturn(List.of(transmisionAbierta()));
        servicio.muestrear();
        when(listener.estaConectado()).thenReturn(true);
    }

    @Test
    void acumula_varias_ventanas_antes_de_clasificar() {
        MuestreoChatService servicio = servicio(15, 180);
        conectar(servicio);
        alGuardarDevolverConId(600L);
        when(listener.tomarYReiniciar())
                .thenReturn(ventana(2, 2, "gg", "vamos"));

        servicio.muestrear();
        servicio.muestrear();
        servicio.muestrear();

        // Las tres muestras se guardan (contar es gratis), pero el bloque de
        // quince minutos no se cumplio: una sola llamada al proveedor por bloque.
        verify(metricaChatRepository, org.mockito.Mockito.times(3)).save(any(MetricaChat.class));
        verify(eventos, never()).publishEvent(any(MuestraChatCapturadaEvent.class));
    }

    @Test
    void al_cerrarse_la_transmision_clasifica_lo_que_quedo_a_medio_bloque() {
        MuestreoChatService servicio = servicio(15, 180);
        conectar(servicio);
        alGuardarDevolverConId(601L);
        when(listener.tomarYReiniciar())
                .thenReturn(ventana(2, 2, "gg", "vamos"));
        servicio.muestrear();

        when(transmisionRepository.findAbiertasParaMuestreo()).thenReturn(List.of());
        servicio.muestrear();

        // Sin esto, una transmision mas corta que el bloque se quedaria sin una
        // sola lectura de sentimiento.
        ArgumentCaptor<MuestraChatCapturadaEvent> publicado =
                ArgumentCaptor.forClass(MuestraChatCapturadaEvent.class);
        verify(eventos).publishEvent(publicado.capture());
        assertThat(publicado.getValue().mensajes()).containsExactly("gg", "vamos");
    }

    @Test
    void se_puede_clasificar_el_bloque_antes_de_que_se_cumpla() {
        MuestreoChatService servicio = servicio(15, 180);
        conectar(servicio);
        alGuardarDevolverConId(603L);
        when(listener.tomarYReiniciar())
                .thenReturn(ventana(2, 2, "gg", "vamos"));
        servicio.muestrear();

        assertThat(servicio.clasificarAhora()).isEqualTo(2);

        ArgumentCaptor<MuestraChatCapturadaEvent> publicado =
                ArgumentCaptor.forClass(MuestraChatCapturadaEvent.class);
        verify(eventos).publishEvent(publicado.capture());
        assertThat(publicado.getValue().mensajes()).containsExactly("gg", "vamos");

        // El bloque quedo consumido: pedirlo de nuevo no reenvia lo mismo.
        assertThat(servicio.clasificarAhora()).isZero();
    }

    @Test
    void clasificar_sin_chat_acumulado_no_publica_nada() {
        MuestreoChatService servicio = servicio(15, 180);

        assertThat(servicio.clasificarAhora()).isZero();

        verify(eventos, never()).publishEvent(any(MuestraChatCapturadaEvent.class));
    }

    @Test
    void recorta_repartiendo_los_mensajes_por_todo_el_bloque() {
        MuestreoChatService servicio = servicio(0, 3);
        conectar(servicio);
        alGuardarDevolverConId(602L);
        when(listener.tomarYReiniciar()).thenReturn(ventana(9, 4, "m0", "m1", "m2", "m3", "m4", "m5", "m6", "m7", "m8"));

        servicio.muestrear();

        ArgumentCaptor<MuestraChatCapturadaEvent> publicado =
                ArgumentCaptor.forClass(MuestraChatCapturadaEvent.class);
        verify(eventos).publishEvent(publicado.capture());

        // Tomar los ultimos tres seria mas simple, pero clasificaria el final del
        // bloque y lo presentaria como el clima de todo el periodo.
        assertThat(publicado.getValue().mensajes()).containsExactly("m0", "m3", "m6");
    }
}
