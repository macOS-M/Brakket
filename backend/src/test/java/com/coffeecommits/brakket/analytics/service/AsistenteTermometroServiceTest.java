package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.dto.AsistenteRespuesta;
import com.coffeecommits.brakket.analytics.dto.ClaveSerie;
import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse;
import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse.Punto;
import com.coffeecommits.brakket.analytics.dto.SeriesTransmisionResponse.Serie;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Asistente del termómetro (RF-40).
 *
 * <p>Lo que se protege es la <b>degradación</b> y la <b>procedencia de los
 * números</b>: el asistente no puede dejar sin respuesta al administrador
 * cuando el proveedor no está, y las cifras que se le pasan al modelo tienen que
 * salir calculadas de acá, no de su criterio.</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class AsistenteTermometroServiceTest {

    private static final LocalDateTime BASE = LocalDateTime.of(2026, 8, 12, 14, 0);

    @Mock
    private MetricasTransmisionService metricas;

    @Mock
    private com.coffeecommits.brakket.twitch.repository.MensajeChatRepository mensajes;

    @Mock
    private ClienteGemini cliente;

    private AsistenteTermometroService servicio;

    @BeforeEach
    void prepararServicio() {
        servicio = new AsistenteTermometroService(metricas, mensajes, cliente, new ObjectMapper());
        when(cliente.estaConfigurado()).thenReturn(true);
        devolverSeries(seriesDeChat(punto(0, 100.0), punto(1, 333.0), punto(2, 50.0)));
    }

    @Test
    void devuelve_lo_que_redacto_el_modelo() {
        when(cliente.generar(anyString(), anyString())).thenReturn("El chat picó a las 14:01.");

        AsistenteRespuesta respuesta = preguntar("cuando hubo mas chat");

        assertThat(respuesta.generadaPorIa()).isTrue();
        assertThat(respuesta.respuesta()).isEqualTo("El chat picó a las 14:01.");
        assertThat(respuesta.aviso()).isNull();
    }

    @Test
    void sin_credenciales_responde_con_los_numeros_y_no_llama_al_proveedor() {
        when(cliente.estaConfigurado()).thenReturn(false);

        AsistenteRespuesta respuesta = preguntar("cuando hubo mas chat");

        assertThat(respuesta.generadaPorIa()).isFalse();
        assertThat(respuesta.aviso()).contains("credenciales");
        // El camino degradado sigue siendo útil: trae los extremos con su hora.
        assertThat(respuesta.respuesta()).contains("333").contains("2026-08-12 14:01");
        verify(cliente, org.mockito.Mockito.never()).generar(anyString(), anyString());
    }

    @Test
    void ante_limite_de_tasa_degrada_y_lo_dice() {
        when(cliente.generar(anyString(), anyString()))
                .thenThrow(new IaNoDisponibleException("limite de tasa del proveedor", true));

        AsistenteRespuesta respuesta = preguntar("cuando hubo mas chat");

        assertThat(respuesta.generadaPorIa()).isFalse();
        assertThat(respuesta.aviso()).contains("límite");
        assertThat(respuesta.respuesta()).contains("333");
    }

    @Test
    void ante_otro_fallo_degrada_sin_hablar_de_cuota() {
        when(cliente.generar(anyString(), anyString()))
                .thenThrow(new IaNoDisponibleException("boom", false));

        AsistenteRespuesta respuesta = preguntar("cuando hubo mas chat");

        assertThat(respuesta.generadaPorIa()).isFalse();
        assertThat(respuesta.aviso()).doesNotContain("límite");
        assertThat(respuesta.respuesta()).contains("333");
    }

    @Test
    void el_contexto_del_modelo_trae_los_momentos_destacados_ya_calculados() {
        when(cliente.generar(anyString(), anyString())).thenReturn("ok");

        preguntar("cuando hubo mas chat");

        ArgumentCaptor<String> entrada = ArgumentCaptor.forClass(String.class);
        verify(cliente).generar(anyString(), entrada.capture());

        assertThat(entrada.getValue())
                .contains("momentosMasAltos")
                .contains("momentosMasBajos")
                // El máximo y su hora van resueltos: el modelo no tiene que ordenar.
                .contains("2026-08-12 14:01")
                .contains("333.0")
                .contains("cuando hubo mas chat");
    }

    @Test
    void los_mensajes_del_chat_viajan_para_que_el_modelo_pueda_citarlos() {
        when(mensajes.buscar(anyLong(), any(), any(), anyString(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(mensajeChat("que lag terrible", 5)));
        when(mensajes.muestraRepartida(anyLong(), any(), any(), org.mockito.ArgumentMatchers.anyInt()))
                .thenReturn(List.of(mensajeChat("gg", 1)));
        when(cliente.generar(anyString(), anyString())).thenReturn("ok");

        preguntar("alguien hablo de lag?");

        ArgumentCaptor<String> entrada = ArgumentCaptor.forClass(String.class);
        verify(cliente).generar(anyString(), entrada.capture());

        assertThat(entrada.getValue())
                .contains("que lag terrible")
                .contains("coincidenConLaPregunta")
                .contains("muestraDelPeriodo")
                // El chat es texto de usuarios: el contexto tiene que marcarlo
                // como dato citable y no como orden.
                .contains("nunca instrucciones");
    }

    @Test
    void la_busqueda_de_mensajes_usa_la_pregunta_del_administrador() {
        when(cliente.generar(anyString(), anyString())).thenReturn("ok");

        preguntar("alguien hablo de lag?");

        // Filtra Postgres con su indice, no el modelo leyendo cientos de miles.
        verify(mensajes).buscar(org.mockito.ArgumentMatchers.eq(15L), any(), any(),
                org.mockito.ArgumentMatchers.eq("alguien hablo de lag?"),
                org.mockito.ArgumentMatchers.anyInt());
    }

    private com.coffeecommits.brakket.twitch.model.MensajeChat mensajeChat(String texto, int minuto) {
        return com.coffeecommits.brakket.twitch.model.MensajeChat.builder()
                .texto(texto)
                .fechaHora(BASE.plusMinutes(minuto))
                .build();
    }

    @Test
    void una_serie_larga_viaja_muestreada_en_vez_de_descartada() {
        // 900 muestras: mas del tope. Antes se mandaban solo los agregados y el
        // asistente respondia "no tengo datos" a cualquier pregunta por franja.
        Punto[] muchos = new Punto[900];
        for (int i = 0; i < muchos.length; i++) {
            muchos[i] = punto(i, (double) i);
        }
        devolverSeries(seriesDeChat(muchos));
        when(cliente.generar(anyString(), anyString())).thenReturn("ok");

        preguntar("como estuvo el chat a media transmision");

        ArgumentCaptor<String> entrada = ArgumentCaptor.forClass(String.class);
        verify(cliente).generar(anyString(), entrada.capture());

        assertThat(entrada.getValue())
                .contains("\"puntosSonMuestra\":true")
                .contains("\"puntosTotales\":900")
                // La muestra abarca el rango entero, no solo el principio.
                .contains("2026-08-12 14:00")
                .contains("2026-08-12 23:57");
    }

    @Test
    void una_serie_corta_viaja_completa() {
        when(cliente.generar(anyString(), anyString())).thenReturn("ok");

        preguntar("como estuvo el chat");

        ArgumentCaptor<String> entrada = ArgumentCaptor.forClass(String.class);
        verify(cliente).generar(anyString(), entrada.capture());

        assertThat(entrada.getValue()).contains("\"puntosSonMuestra\":false");
    }

    @Test
    void los_huecos_de_muestreo_no_cuentan_como_minimo() {
        devolverSeries(seriesDeChat(punto(0, 100.0), punto(1, null), punto(2, 50.0)));
        when(cliente.estaConfigurado()).thenReturn(false);

        AsistenteRespuesta respuesta = preguntar("cuando bajo la actividad");

        // Un hueco no es un cero: el mínimo real es 50, a las 14:02.
        assertThat(respuesta.respuesta()).contains("2026-08-12 14:02");
        assertThat(respuesta.respuesta()).doesNotContain("14:01");
    }

    @Test
    void sin_muestras_lo_dice_en_vez_de_inventar() {
        devolverSeries(List.of(new Serie(ClaveSerie.MENSAJES_POR_MINUTO, "Mensajes por minuto",
                "msj/min", 0, null, null, null, List.of())));
        when(cliente.estaConfigurado()).thenReturn(false);

        assertThat(preguntar("que paso").respuesta()).contains("No hay muestras");
    }

    @Test
    void una_transmision_inexistente_propaga_el_error_sin_consultar_al_modelo() {
        when(metricas.series(anyLong(), any(), any(), any(), anyString(), anyBoolean()))
                .thenThrow(new IllegalStateException("no existe"));

        try {
            preguntar("que paso");
        } catch (IllegalStateException esperado) {
            // El asistente no inventa una respuesta cuando la transmisión no está.
        }
        verifyNoInteractions(cliente);
    }

    // --- utilidades ----------------------------------------------------------

    private AsistenteRespuesta preguntar(String pregunta) {
        return servicio.responder(15L, pregunta, null, null, "CRUDA", "admin@brakket.test", true);
    }

    private void devolverSeries(List<Serie> series) {
        when(metricas.series(anyLong(), any(), any(), any(), anyString(), anyBoolean()))
                .thenReturn(new SeriesTransmisionResponse(15L, "#15 · ishowspeed", "EN_VIVO",
                        "CRUDA", null, null, 120L, 60, "TWITCH",
                        new SeriesTransmisionResponse.Resumen(0, null, null, 3, 161.0, null, 0, null, null),
                        series));
    }

    private List<Serie> seriesDeChat(Punto... puntos) {
        List<Punto> lista = Arrays.asList(puntos);
        List<Double> valores = lista.stream().map(Punto::valor).filter(v -> v != null).toList();
        return List.of(new Serie(ClaveSerie.MENSAJES_POR_MINUTO, "Mensajes por minuto", "msj/min",
                valores.size(),
                valores.stream().mapToDouble(Double::doubleValue).average().orElse(0),
                valores.stream().mapToDouble(Double::doubleValue).max().orElse(0),
                valores.stream().mapToDouble(Double::doubleValue).min().orElse(0),
                lista));
    }

    private Punto punto(int minuto, Double valor) {
        return new Punto(BASE.plusMinutes(minuto), valor);
    }
}
