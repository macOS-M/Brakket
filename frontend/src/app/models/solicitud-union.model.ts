/** Solicitud de unión a un equipo (flujo inverso a la invitación). */
export interface SolicitudUnion {
  id: number;
  equipoId: number;
  equipoNombre: string;
  jugadorId: number;
  jugadorNombre: string;
  mensaje: string | null;
  estado: string;
  fechaCreacion: string;
  fechaRespuesta: string | null;
}
