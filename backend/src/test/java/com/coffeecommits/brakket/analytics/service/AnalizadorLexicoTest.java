package com.coffeecommits.brakket.analytics.service;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;
import com.coffeecommits.brakket.analytics.service.AnalizadorSentimiento.Resultado;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AnalizadorLexicoTest {

    private final AnalizadorLexico analizador = new AnalizadorLexico();

    /** U+2764 pelado: el corazón "de texto". */
    private static final String CORAZON = "\u2764";
    /** U+2764 U+FE0F: el que emiten los teclados y Twitch por defecto. */
    private static final String CORAZON_EMOJI = "\u2764\uFE0F";

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

    @Test
    void el_corazon_con_presentacion_emoji_cuenta_una_sola_vez() {
        // Contra el doble conteo: "❤️" contiene "❤", así que con ambas variantes
        // en el léxico un solo corazón sumaba +2 y sesgaba el lote a POSITIVO.
        // 1 positivo (corazón) y 1 negativo (lag) ⇒ empate ⇒ NEUTRO.
        Resultado r = analizador.analizar(List.of(CORAZON_EMOJI + " pero mucho lag"));

        assertThat(r.puntaje()).isEqualByComparingTo("0.00");
        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.NEUTRO);
    }

    @Test
    void las_dos_variantes_del_corazon_puntuan_igual() {
        Resultado pelado = analizador.analizar(List.of(CORAZON + " pero mucho lag"));
        Resultado conSelector = analizador.analizar(List.of(CORAZON_EMOJI + " pero mucho lag"));

        assertThat(conSelector.puntaje()).isEqualByComparingTo(pelado.puntaje());
    }

    @Test
    void los_emojis_se_cuentan_por_ocurrencia_igual_que_las_palabras() {
        // 3 emojis positivos y 1 palabra negativa ⇒ (3-1)/4 * 100 = 50.
        // Contándolos como booleano por mensaje habría dado 1 y 1 ⇒ 0.
        Resultado r = analizador.analizar(List.of("🔥🔥🔥 pero lag"));

        assertThat(r.puntaje()).isEqualByComparingTo("50.00");
        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.POSITIVO);
    }

    @Test
    void un_emoji_repetido_pesa_lo_mismo_que_la_palabra_repetida() {
        Resultado emojis = analizador.analizar(List.of("🔥🔥🔥🔥🔥 lag lag"));
        Resultado palabras = analizador.analizar(List.of("gg gg gg gg gg lag lag"));

        assertThat(emojis.puntaje()).isEqualByComparingTo(palabras.puntaje());
    }

    @Test
    void los_emojis_negativos_tambien_suman_por_ocurrencia() {
        // 1 positivo (gg) y 3 negativos (👎) ⇒ (1-3)/4 * 100 = -50.
        Resultado r = analizador.analizar(List.of("gg 👎👎👎"));

        assertThat(r.puntaje()).isEqualByComparingTo("-50.00");
        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.NEGATIVO);
    }

    @Test
    void los_terminos_ambiguos_no_puntuan() {
        // "ez" suele ser burla al rival y "wtf" tan seguido es sorpresa positiva
        // como queja: salieron del léxico por meter más ruido del que aportaban.
        Resultado r = analizador.analizar(List.of("ez", "wtf"));

        assertThat(r.puntaje()).isEqualByComparingTo("0.00");
        assertThat(r.clasificacion()).isEqualTo(ClasificacionSentimiento.NEUTRO);
    }
}
