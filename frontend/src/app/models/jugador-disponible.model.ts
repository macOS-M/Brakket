export interface JugadorDisponible {
  id: number;
  nombre: string;
  fotoUrl: string | null;
  requiereTransferencia: boolean;
  equipoActualNombre: string | null;
}

export interface InvitarJugadorRequest {
  jugadorId: number;
  rolPropuesto: string;
  mensaje: string | null;
}
