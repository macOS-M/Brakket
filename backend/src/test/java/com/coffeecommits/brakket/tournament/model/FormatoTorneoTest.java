package com.coffeecommits.brakket.tournament.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * El torneo guarda el formato como texto libre: lo que la interfaz muestre es
 * lo que termina en la base. Al traducir "Round robin" a "Todos contra todos"
 * (la app va enteramente en español), el reconocimiento tenía que aceptar el
 * nombre nuevo sin dejar de entender el viejo, o los torneos ya creados
 * habrían pasado a correr como eliminación directa.
 */
class FormatoTorneoTest {

    @Test
    @DisplayName("reconoce el nombre nuevo en español")
    void reconoceTodosContraTodos() {
        assertThat(FormatoTorneo.interpretar("Todos contra todos"))
                .contains(FormatoTorneo.ROUND_ROBIN);
    }

    @Test
    @DisplayName("sigue reconociendo el nombre viejo de los torneos ya creados")
    void reconoceRoundRobin() {
        assertThat(FormatoTorneo.interpretar("Round robin"))
                .contains(FormatoTorneo.ROUND_ROBIN);
        assertThat(FormatoTorneo.interpretar("ROUND_ROBIN"))
                .contains(FormatoTorneo.ROUND_ROBIN);
    }

    @Test
    @DisplayName("los demás formatos siguen resolviéndose igual")
    void reconoceElResto() {
        assertThat(FormatoTorneo.interpretar("Eliminación directa"))
                .contains(FormatoTorneo.ELIMINACION_DIRECTA);
        assertThat(FormatoTorneo.interpretar("Doble eliminación"))
                .contains(FormatoTorneo.DOBLE_ELIMINACION);
        assertThat(FormatoTorneo.interpretar("Suizo"))
                .contains(FormatoTorneo.SUIZO);
        assertThat(FormatoTorneo.interpretar("Fase de grupos y eliminación"))
                .contains(FormatoTorneo.FASE_GRUPOS_Y_ELIMINACION);
    }

    @Test
    @DisplayName("la fase de grupos gana sobre eliminación, y todos contra todos sobre doble")
    void respetaLaPrecedencia() {
        assertThat(FormatoTorneo.interpretar("Fase de grupos y eliminación directa"))
                .contains(FormatoTorneo.FASE_GRUPOS_Y_ELIMINACION);
        assertThat(FormatoTorneo.interpretar("Todos contra todos doble"))
                .contains(FormatoTorneo.ROUND_ROBIN);
    }

    @Test
    @DisplayName("un texto sin relación no se interpreta")
    void noInterpretaCualquierCosa() {
        assertThat(FormatoTorneo.interpretar("formato inventado")).isEmpty();
        assertThat(FormatoTorneo.interpretar("")).isEqualTo(Optional.empty());
        assertThat(FormatoTorneo.interpretar(null)).isEmpty();
    }
}
