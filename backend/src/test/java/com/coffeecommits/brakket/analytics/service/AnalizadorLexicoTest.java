package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.analytics.service.AnalizadorSentimiento.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalizadorLexicoTest {

    private final AnalizadorLexico analizador = new AnalizadorLexico();

    @Test
    void lote_positivo_clasifica_positivo_con_puntaje_maximo() {
        Resultado r = analizador.analizar(List.of(
                "GG equipo, jugada increíble!", "pog pog vamos", "clutch 🔥"));

        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.POSITIVO);
        assertThat(r.puntaje()).isEqualByComparingTo("100.00");
    }

    @Test
    void lote_negativo_clasifica_negativo_con_puntaje_minimo() {
        Resultado r = analizador.analizar(List.of(
                "esto es basura, puro lag", "cheater rip", "aburrido 👎"));

        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.NEGATIVO);
        assertThat(r.puntaje()).isEqualByComparingTo("-100.00");
    }

    @Test
    void lote_sin_senal_es_neutro_con_puntaje_cero() {
        Resultado r = analizador.analizar(List.of(
                "hola a todos", "alguien sabe el marcador", "que hora empieza"));

        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.NEUTRO);
        assertThat(r.puntaje()).isEqualByComparingTo("0.00");
    }

    @Test
    void lote_balanceado_queda_neutro() {
        // 1 positivo (gg) y 1 negativo (lag) ⇒ puntaje 0.
        Resultado r = analizador.analizar(List.of("gg pero mucho lag"));

        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.NEUTRO);
        assertThat(r.puntaje()).isEqualByComparingTo("0.00");
    }

    @Test
    void proporcion_determina_el_puntaje() {
        // 3 positivos, 1 negativo ⇒ (3-1)/4 * 100 = 50 ⇒ POSITIVO.
        Resultado r = analizador.analizar(List.of("gg", "gg", "gg", "lag"));

        assertThat(r.puntaje()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.POSITIVO);
    }
}
