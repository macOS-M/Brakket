package com.coffeecommits.brakket.game.service;

import com.coffeecommits.brakket.common.exception.BusinessException;
import com.coffeecommits.brakket.twitch.service.TwitchTokenProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IgdbGameSearchServiceImplTest {

    private static final String BASE_URL = "https://api.igdb.com/v4";

    private TwitchTokenProvider tokenProvider;
    private MockRestServiceServer servidor;
    private IgdbGameSearchServiceImpl servicio;

    @BeforeEach
    void prepararServicio() {
        tokenProvider = mock(TwitchTokenProvider.class);
        when(tokenProvider.configurado()).thenReturn(true);
        when(tokenProvider.obtener(false)).thenReturn("token-de-prueba");
        when(tokenProvider.clientId()).thenReturn("client-de-prueba");

        RestClient.Builder builder = RestClient.builder().baseUrl(BASE_URL);
        servidor = MockRestServiceServer.bindTo(builder).build();
        servicio = new IgdbGameSearchServiceImpl(builder.build(), tokenProvider);
    }

    @Test
    void buscar_falla_con_mensaje_claro_sin_credenciales_de_twitch() {
        when(tokenProvider.configurado()).thenReturn(false);

        assertThatThrownBy(() -> servicio.buscar("valorant"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("TWITCH_CLIENT_ID");
    }

    @Test
    void buscar_devuelve_vacio_sin_llamar_a_la_red_con_consulta_corta() {
        // Sin expectativas registradas en el servidor: si saliera a la red,
        // el verify() de abajo fallaría.
        assertThat(servicio.buscar("v")).isEmpty();
        assertThat(servicio.buscar("  ")).isEmpty();
        assertThat(servicio.buscar(null)).isEmpty();
        servidor.verify();
    }

    @Test
    void buscar_mapea_nombre_genero_traducido_y_arma_la_url_de_la_portada() {
        servidor.expect(requestTo(BASE_URL + "/games"))
                .andExpect(header("Client-ID", "client-de-prueba"))
                .andExpect(header("Authorization", "Bearer token-de-prueba"))
                .andRespond(withSuccess("""
                        [{"name":"Valorant","slug":"valorant",
                          "cover":{"image_id":"co2mvx"},
                          "genres":[{"name":"Shooter"}]}]
                        """, MediaType.APPLICATION_JSON));

        var resultados = servicio.buscar("valorant");

        assertThat(resultados).hasSize(1);
        assertThat(resultados.get(0).nombre()).isEqualTo("Valorant");
        assertThat(resultados.get(0).slug()).isEqualTo("valorant");
        assertThat(resultados.get(0).genero()).isEqualTo("Shooter");
        assertThat(resultados.get(0).imagenUrl())
                .isEqualTo("https://images.igdb.com/igdb/image/upload/t_cover_big_2x/co2mvx.jpg");
        servidor.verify();
    }

    @Test
    void buscar_pone_primero_la_coincidencia_exacta_aunque_igdb_la_devuelva_despues() {
        // Caso real: buscando "Valorant", IGDB rankea antes "Grit & Valor:
        // 1949". El seeder se queda con el primero, así que sin reordenar
        // sembraría el juego equivocado.
        servidor.expect(requestTo(BASE_URL + "/games"))
                .andRespond(withSuccess("""
                        [{"name":"Grit & Valor: 1949","slug":"grit-and-valor-1949"},
                         {"name":"Valorant","slug":"valorant"},
                         {"name":"Valor of Man","slug":"valor-of-man"}]
                        """, MediaType.APPLICATION_JSON));

        var resultados = servicio.buscar("valorant");

        assertThat(resultados.get(0).nombre()).isEqualTo("Valorant");
        // El resto conserva el orden de relevancia de IGDB.
        assertThat(resultados).extracting("nombre")
                .containsExactly("Valorant", "Grit & Valor: 1949", "Valor of Man");
        servidor.verify();
    }

    @Test
    void buscar_quita_las_comillas_para_no_romper_la_consulta_apicalypse() {
        // APICalypse no tiene parámetros ligados: una comilla sin sanear
        // cerraría el search y el resto del query quedaría suelto.
        servidor.expect(requestTo(BASE_URL + "/games"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("search \"Halo Infinite\";")))
                .andRespond(withSuccess("[]", MediaType.APPLICATION_JSON));

        assertThat(servicio.buscar("Halo \"Infinite\"")).isEmpty();
        servidor.verify();
    }

    @Test
    void detalle_convierte_rating_a_escala_cinco_fecha_y_capturas() {
        servidor.expect(requestTo(BASE_URL + "/games"))
                .andRespond(withSuccess("""
                        [{"summary":"Shooter tactico 5v5",
                          "first_release_date":1591056000,
                          "rating":92.0,
                          "aggregated_rating":88.4,
                          "websites":[{"url":"https://www.youtube.com/@valorant","type":9},
                                      {"url":"https://playvalorant.com","type":1},
                                      {"url":"https://twitter.com/VALORANT","type":5}],
                          "platforms":[{"name":"PC (Microsoft Windows)"}],
                          "themes":[{"name":"Action"}],
                          "screenshots":[{"image_id":"sc8xyz"}]}]
                        """, MediaType.APPLICATION_JSON));

        var detalle = servicio.detalle("valorant");

        assertThat(detalle).isNotNull();
        assertThat(detalle.descripcion()).isEqualTo("Shooter tactico 5v5");
        assertThat(detalle.fechaLanzamiento()).isEqualTo(LocalDate.of(2020, 6, 2));
        // 92 sobre 100 en IGDB es 4.6 sobre 5 en la escala que usa el frontend.
        assertThat(detalle.rating()).isEqualTo(4.6);
        assertThat(detalle.metacritic()).isEqualTo(88);
        // IGDB devuelve las redes sin orden útil: el oficial es el de tipo 1,
        // no el primero (que en el caso real es el canal de YouTube).
        assertThat(detalle.sitioWeb()).isEqualTo("https://playvalorant.com");
        assertThat(detalle.plataformas()).containsExactly("PC (Microsoft Windows)");
        assertThat(detalle.etiquetas()).containsExactly("Action");
        assertThat(detalle.capturas())
                .containsExactly("https://images.igdb.com/igdb/image/upload/t_screenshot_huge/sc8xyz.jpg");
        servidor.verify();
    }

    @Test
    void detalle_devuelve_null_si_igdb_falla_para_no_tumbar_el_import() {
        servidor.expect(requestTo(BASE_URL + "/games")).andRespond(withServerError());

        assertThat(servicio.detalle("valorant")).isNull();
        servidor.verify();
    }

    @Test
    void populares_traduce_la_caida_de_igdb_en_un_error_de_negocio() {
        servidor.expect(requestTo(BASE_URL + "/games")).andRespond(withServerError());

        assertThatThrownBy(() -> servicio.populares())
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("no está disponible");
        servidor.verify();
    }
}
