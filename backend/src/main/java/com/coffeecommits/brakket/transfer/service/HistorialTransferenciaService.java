package com.coffeecommits.brakket.transfer.service;

import com.coffeecommits.brakket.auth.model.Usuario;
import com.coffeecommits.brakket.transfer.model.SolicitudTransferencia;

/**
 * Registro histórico automático de transferencias completadas (RF-14).
 */
public interface HistorialTransferenciaService {

    /**
     * Crea el registro histórico de una transferencia recién aprobada.
     * Debe llamarse dentro de la misma transacción que ejecuta el cambio
     * de plantilla, para que un fallo aquí revierta la transferencia
     * completa (criterio RF-14).
     */
    void registrar(SolicitudTransferencia solicitud, Usuario responsable);
}