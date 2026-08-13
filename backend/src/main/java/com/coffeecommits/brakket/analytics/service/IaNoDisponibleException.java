package com.coffeecommits.brakket.analytics.service;

/**
 * El proveedor de IA no pudo responder.
 *
 * <p>Nunca llega al cliente HTTP: quien la atrapa degrada a la respuesta
 * determinista. Existe para <b>distinguir el motivo</b>, y en particular para
 * separar el límite de tasa de una falla real. Sin esa distinción, agotar la
 * cuota gratuita durante una transmisión larga se vería en el log igual que un
 * modelo mal configurado, y no habría forma de medirlo.</p>
 */
public class IaNoDisponibleException extends RuntimeException {

    private final boolean porLimiteDeTasa;

    public IaNoDisponibleException(String mensaje, boolean porLimiteDeTasa) {
        super(mensaje);
        this.porLimiteDeTasa = porLimiteDeTasa;
    }

    public IaNoDisponibleException(String mensaje, Throwable causa) {
        super(mensaje, causa);
        this.porLimiteDeTasa = false;
    }

    /** true si el proveedor devolvió 429: hay cuota agotada, no un error. */
    public boolean isPorLimiteDeTasa() {
        return porLimiteDeTasa;
    }
}
