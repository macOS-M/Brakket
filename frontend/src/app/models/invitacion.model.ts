export interface Invitacion {
  id: number;
  equipoId: number;
  equipoNombre: string;
  jugadorId: number;
  jugadorNombre: string;
  rolPropuesto: string;
  mensaje: string | null;
  estado: string;
  creadoPorId: number;
  creadoPorNombre: string;
  fechaCreacion: string;
  fechaRespuesta: string | null;
}

export interface InvitarJugadorRequest {
  jugadorId: number;
  rolPropuesto: string;
  mensaje: string | null;
}

export interface ResponderInvitacionRequest {
  aceptar: boolean;
}
