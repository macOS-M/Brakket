package com.coffeecommits.brakket.analytics.dto;

import com.coffeecommits.brakket.analytics.model.ClasificacionSentimiento;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Reparto de las muestras entre las tres clasificaciones (RF-40).
 *
 * <p>Es la "distribución" que la ERS pide como dato de salida: el puntaje
 * general solo dice dónde quedó el promedio, y un chat partido en mitades
 * entusiastas y hostiles promedia igual que uno tibio. Sin este reparto, los dos
 * se verían idénticos en el termómetro.</p>
 */
public record DistribucionSentimiento(
        long positivo,
        long neutro,
        long negativo,
        BigDecimal porcentajePositivo,
        BigDecimal porcentajeNeutro,
        BigDecimal porcentajeNegativo
) {

    /** Cuenta las clasificaciones de la serie y calcula sus porcentajes. */
    public static DistribucionSentimiento de(List<String> clasificaciones) {
        long positivo = contar(clasificaciones, ClasificacionSentimiento.POSITIVO);
        long neutro = contar(clasificaciones, ClasificacionSentimiento.NEUTRO);
        long negativo = contar(clasificaciones, ClasificacionSentimiento.NEGATIVO);
        long total = clasificaciones.size();

        return new DistribucionSentimiento(positivo, neutro, negativo,
                porcentaje(positivo, total), porcentaje(neutro, total), porcentaje(negativo, total));
    }

    private static long contar(List<String> clasificaciones, ClasificacionSentimiento cual) {
        return clasificaciones.stream().filter(c -> cual.name().equals(c)).count();
    }

    private static BigDecimal porcentaje(long parte, long total) {
        if (total == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(100L * parte)
                .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP);
    }
}
