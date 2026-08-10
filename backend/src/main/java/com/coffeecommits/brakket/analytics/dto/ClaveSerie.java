package com.coffeecommits.brakket.analytics.dto;

/** RF-37: las series que devuelve el endpoint. Siempre viajan las cuatro. */
public enum ClaveSerie {
    ESPECTADORES("Espectadores", "espectadores"),
    MENSAJES_POR_MINUTO("Mensajes por minuto", "msj/min"),
    USUARIOS_ACTIVOS("Usuarios activos", "usuarios"),
    SENTIMIENTO("Sentimiento", null);

    private final String etiqueta;
    private final String unidad;

    ClaveSerie(String etiqueta, String unidad) {
        this.etiqueta = etiqueta;
        this.unidad = unidad;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public String unidad() {
        return unidad;
    }
}
