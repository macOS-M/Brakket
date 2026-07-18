/** Solicitud de transferencia de un jugador entre equipos (RF-12/RF-13). */
export interface Transferencia {
  id: number;
  jugadorId: number;
  jugadorNombre: string;
  equipoOrigenId: number;
  equipoOrigenNombre: string;
  equipoDestinoId: number;
  equipoDestinoNombre: string;
  solicitanteId: number;
  solicitanteNombre: string;
  rolPropuesto: string;
  justificacion: string | null;
  /** PENDIENTE / APROBADA / RECHAZADA */
  estado: string;
  /** PENDIENTE / ACEPTADA / RECHAZADA */
  aprobacionJugador: string;
  aprobacionCapitanOrigen: string;
  fechaSolicitud: string;
  fechaResolucion: string | null;
}

export interface CrearTransferenciaRequest {
  jugadorId: number;
  equipoOrigenId: number;
  equipoDestinoId: number;
  rolPropuesto: string;
  justificacion: string | null;
}

export const ROLES_PROPUESTOS = ['TITULAR', 'SUPLENTE', 'COACH'] as const;
