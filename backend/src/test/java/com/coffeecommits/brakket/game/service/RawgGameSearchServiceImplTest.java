package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawgGameSearchServiceImplTest {

    private static final String BASE_URL = "https://api.rawg.io/api";

    private RawgGameSearchServiceImpl servicio(String apiKey) {
        return new RawgGameSearchServiceImpl(RestClient.builder(), BASE_URL, apiKey);
    }

    @Test
    void buscar_falla_con_mensaje_claro_si_no_hay_api_key() {
        assertThatThrownBy(() -> servicio("").buscar("valorant"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("RAWG_API_KEY");
    }

    @Test
    void buscar_devuelve_vacio_sin_llamar_a_la_red_con_consulta_corta() {
        // Con una consulta de menos de 2 caracteres no se toca la API:
        // si se intentara, la key falsa provocaría un error de red.
        assertThat(servicio("key-falsa").buscar("v")).isEmpty();
        assertThat(servicio("key-falsa").buscar("  ")).isEmpty();
        assertThat(servicio("key-falsa").buscar(null)).isEmpty();
    }
}
