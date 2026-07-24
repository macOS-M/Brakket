package com.coffeecommits.brakket.tournament.service;

import com.coffeecommits.brakket.team.model.Equipo;
import com.coffeecommits.brakket.tournament.model.EstadoPartida;
import com.coffeecommits.brakket.tournament.model.Partida;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Clasificación por marcas para los formatos de liga (round robin, suizo y
 * fase de grupos). Solo cuentan partidas FINALIZADAS; un bye vale una
 * victoria sin marcador. Desempate: victorias, diferencia de puntos, puntos
 * a favor y, de persistir, el id de equipo (el único criterio que el
 * frontend puede reproducir desde el bracket público).
 *
 * <p>El frontend calcula la misma tabla a partir del bracket público; si se
 * cambia un criterio aquí hay que reflejarlo allá.</p>
 */
final class TablaPosiciones {

    /** Marcas de un equipo dentro del conjunto de partidas evaluado. */
    record Posicion(Equipo equipo, int jugadas, int ganadas, int perdidas, int favor, int contra) {
        int diferencia() {
            return favor - contra;
        }
    }

    private TablaPosiciones() {
    }

    /** Tabla ordenada de mejor a peor. {@code equipos} fija el orden de inscripción. */
    static List<Posicion> calcular(List<Equipo> equipos, List<Partida> partidas) {
        Map<Long, int[]> marcas = new LinkedHashMap<>(); // {pj, g, p, favor, contra}
        for (Equipo equipo : equipos) {
            marcas.put(equipo.getId(), new int[5]);
        }
        for (Partida p : partidas) {
            if (p.getEstado() != EstadoPartida.FINALIZADA || p.getGanador() == null) {
                continue;
            }
            acumular(marcas, p.getEquipoA(), p, true);
            acumular(marcas, p.getEquipoB(), p, false);
        }
        List<Posicion> tabla = new ArrayList<>();
        for (Equipo equipo : equipos) {
            int[] m = marcas.get(equipo.getId());
            tabla.add(new Posicion(equipo, m[0], m[1], m[2], m[3], m[4]));
        }
        tabla.sort(Comparator
                .comparingInt(Posicion::ganadas).reversed()
                .thenComparing(Comparator.comparingInt(Posicion::diferencia).reversed())
                .thenComparing(Comparator.comparingInt(Posicion::favor).reversed())
                .thenComparing(p -> p.equipo().getId()));
        return tabla;
    }

    private static void acumular(Map<Long, int[]> marcas, Equipo equipo, Partida p, boolean ladoA) {
        if (equipo == null) {
            return;
        }
        int[] m = marcas.get(equipo.getId());
        if (m == null) {
            return; // equipo retirado del torneo: sus partidas viejas no suman
        }
        m[0]++;
        if (equipo.getId().equals(p.getGanador().getId())) {
            m[1]++;
        } else {
            m[2]++;
        }
        if (p.getMarcadorA() != null && p.getMarcadorB() != null) {
            m[3] += ladoA ? p.getMarcadorA() : p.getMarcadorB();
            m[4] += ladoA ? p.getMarcadorB() : p.getMarcadorA();
        }
    }
}
