package com.coffeecommits.brakket.twitch.model;

/**
 * Plataforma de origen de una transmisión (RF-35).
 * Hoy solo Twitch tiene proveedor implementado; YouTube y Kick existen para
 * que el esquema y los DTO no cambien cuando lleguen sus adaptadores (RNF-23).
 */
public enum PlataformaTransmision {
    TWITCH, YOUTUBE, KICK
}
