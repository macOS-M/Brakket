/**
 * Modelos de datos del RF-19 (Control de roles y permisos).
 */

export interface RolDTO {
  id: number;
  nombreRol: string;
  nivel: number;
  permisos: string[];
}

export interface UsuarioRolesDTO {
  usuarioId: number;
  correo: string;
  perfilCompleto: boolean;
  roles: string[];
  permisos: string[];
}

export interface AsignarRolRequest {
  rolId: number;
}

/** Forma de las respuestas de error que arma el GlobalExceptionHandler. */
export interface ApiErrorBody {
  success: boolean;
  message: string;
  data: unknown;
  errors: Record<string, string> | null;
  timestamp: string;
}
