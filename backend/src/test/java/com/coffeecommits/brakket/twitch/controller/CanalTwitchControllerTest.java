package com.coffeecommits.brakket.twitch.controller;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.common.exception.GlobalExceptionHandler;
import com.coffeecommits.brakket.twitch.dto.AsociarTransmisionRequest;
import com.coffeecommits.brakket.twitch.dto.MetricasTransmisionResponse;
import com.coffeecommits.brakket.twitch.dto.TransmisionTwitchResponse;
import com.coffeecommits.brakket.twitch.service.CanalTwitchService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del controlador REST del canal de Twitch (RF-34/RF-36).
 *
 * <p>Igual que TorneoControllerTest, se monta con {@code standaloneSetup}: lo
 * que se verifica es el contrato HTTP —ruta, variable de path, cuerpo JSON y
 * código de estado—, no la lógica del servicio.</p>
 */
@ExtendWith(MockitoExtension.class)
class CanalTwitchControllerTest {

    @Mock
    private CanalTwitchService service;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        MappingJackson2HttpMessageConverter conversor = new MappingJackson2HttpMessageConverter(
                JsonMapper.builder().addModule(new JavaTimeModule()).build());

        mockMvc = MockMvcBuilders.standaloneSetup(new CanalTwitchController(service))
                .setMessageConverters(conversor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void metricas_toma_el_id_de_la_ruta_y_devuelve_los_indicadores() throws Exception {
        when(service.metricas(7L)).thenReturn(new MetricasTransmisionResponse(
                7L, "FINALIZADA", 5L, 1820, 934.5, 240L,
                LocalDateTime.of(2026, 7, 24, 18, 0),
                LocalDateTime.of(2026, 7, 24, 22, 0),
                LocalDateTime.of(2026, 7, 24, 21, 59)));

        mockMvc.perform(get("/api/twitch/transmisiones/7/metricas"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transmisionId").value(7))
                .andExpect(jsonPath("$.muestras").value(5))
                .andExpect(jsonPath("$.pico").value(1820))
                .andExpect(jsonPath("$.promedio").value(934.5));

        verify(service).metricas(7L);
    }

    @Test
    void transmisiones_abiertas_devuelve_la_lista() throws Exception {
        when(service.listarAbiertas()).thenReturn(List.of(new TransmisionTwitchResponse(
                7L, "317466024803", 27L, null, "EN_VIVO",
                LocalDateTime.of(2026, 7, 24, 18, 0))));

        mockMvc.perform(get("/api/twitch/transmisiones"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].estado").value("EN_VIVO"))
                .andExpect(jsonPath("$[0].torneoId").value(27));
    }

    @Test
    void asociar_deserializa_el_cuerpo_y_lo_pasa_al_servicio() throws Exception {
        when(service.asociar(any())).thenReturn(new TransmisionTwitchResponse(
                8L, null, 27L, null, "SIN_DATOS_EN_VIVO", null));

        mockMvc.perform(post("/api/twitch/transmisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"torneoId\":27,\"partidaId\":null}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(8))
                .andExpect(jsonPath("$.estado").value("SIN_DATOS_EN_VIVO"));

        verify(service).asociar(new AsociarTransmisionRequest(27L, null));
    }

    /**
     * Los errores de negocio salen como 409 por GlobalExceptionHandler, no como
     * un 500: es el contrato que el panel del frontend interpreta.
     */
    @Test
    void un_error_de_negocio_del_servicio_responde_409() throws Exception {
        when(service.asociar(any()))
                .thenThrow(new BusinessException("No existe un canal oficial activo."));

        mockMvc.perform(post("/api/twitch/transmisiones")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"torneoId\":27}"))
                .andExpect(status().isConflict());
    }
}
