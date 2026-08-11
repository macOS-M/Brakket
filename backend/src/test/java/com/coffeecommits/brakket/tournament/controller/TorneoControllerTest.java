package com.coffeecommits.brakket.tournament.controller;

import com.coffeecommits.brakket.common.exception.GlobalExceptionHandler;
import com.coffeecommits.brakket.common.exception.ResourceNotFoundException;
import com.coffeecommits.brakket.tournament.dto.TorneoResponse;
import com.coffeecommits.brakket.tournament.service.PartidaService;
import com.coffeecommits.brakket.tournament.service.TorneoService;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas del controlador REST de torneos (RF-24/RF-25).
 *
 * <p>Se monta con {@code standaloneSetup} en vez de levantar el contexto de
 * Spring: alcanza para ejercitar lo que le toca a un controlador —ruteo,
 * binding de parámetros, serialización a JSON y código de estado— y corre en
 * milisegundos. La lógica de negocio ya está cubierta por TorneoServiceImplTest.</p>
 */
@ExtendWith(MockitoExtension.class)
class TorneoControllerTest {

    private static final String CORREO = "orga.demo@brakket.gg";

    @Mock
    private TorneoService torneoService;
    @Mock
    private PartidaService partidaService;

    private MockMvc mockMvc;
    private TestingAuthenticationToken usuario;

    @BeforeEach
    void setUp() {
        // El ObjectMapper por defecto de standaloneSetup no sabe serializar
        // LocalDateTime; se le agrega el módulo de java.time explícitamente.
        MappingJackson2HttpMessageConverter conversor = new MappingJackson2HttpMessageConverter(
                JsonMapper.builder().addModule(new JavaTimeModule()).build());

        mockMvc = MockMvcBuilders.standaloneSetup(new TorneoController(torneoService, partidaService))
                .setMessageConverters(conversor)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        usuario = new TestingAuthenticationToken(CORREO, "n/a", "ROLE_USER");
    }

    @Test
    void listar_pasa_el_filtro_de_juego_y_la_identidad_al_servicio() throws Exception {
        when(torneoService.listar(3L, CORREO)).thenReturn(List.of(torneo()));

        mockMvc.perform(get("/api/tournaments").param("juegoId", "3").principal(usuario))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nombre").value("Copa Relampago (Demo)"))
                .andExpect(jsonPath("$[0].estado").value("EN_CURSO"));

        verify(torneoService).listar(3L, CORREO);
    }

    @Test
    void listar_sin_sesion_consulta_como_anonimo() throws Exception {
        when(torneoService.listar(eq(null), any())).thenReturn(List.of());

        mockMvc.perform(get("/api/tournaments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));

        // Sin principal el correo viaja nulo: las lecturas públicas no exigen sesión.
        verify(torneoService).listar(null, null);
    }

    @Test
    void obtener_un_torneo_inexistente_responde_404() throws Exception {
        when(torneoService.obtenerDetalle(eq(99L), any(), eq(false)))
                .thenThrow(new ResourceNotFoundException("Torneo", 99L));

        mockMvc.perform(get("/api/tournaments/99").principal(usuario))
                .andExpect(status().isNotFound());
    }

    private TorneoResponse torneo() {
        return new TorneoResponse(
                7L, "Copa Relampago (Demo)", "Torneo de demostración",
                3L, "Rocket League", "https://cdn/rl.jpg",
                1L, "Liga Demo Brakket", 1L, "Temporada Demo 2026",
                5L, "Olivia Organizadora", "ELIMINACION_DIRECTA",
                1, 4, 4L,
                LocalDateTime.of(2026, 7, 24, 18, 0), "EN_CURSO", true, "Gloria eterna",
                List.of(), null, null);
    }
}
