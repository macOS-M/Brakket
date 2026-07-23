export interface MiembroEquipo {
  id: number;
  equipoId: number;
  usuarioId: number;
  nombreUsuario: string;
  rol: string;
  estado: string;
  /** Fecha de incorporación a la plantilla (RF-08). */
  fechaUnion: string | null;
}

export interface AsignarRolRequest {
  nuevoRol: string;
}

/** RF-10: la causa de la expulsión es obligatoria. */
export interface ExpulsarIntegranteRequest {
  causa: string;
}

export const ROLES_EQUIPO = ['CAPITAN', 'TITULAR', 'SUPLENTE', 'COACH'] as const;
