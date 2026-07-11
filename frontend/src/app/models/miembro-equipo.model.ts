export interface MiembroEquipo {
  id: number;
  equipoId: number;
  usuarioId: number;
  nombreUsuario: string;
  rol: string;
  estado: string;
}

export interface AsignarRolRequest {
  nuevoRol: string;
}

export const ROLES_EQUIPO = ['CAPITAN', 'TITULAR', 'SUPLENTE', 'COACH'] as const;
