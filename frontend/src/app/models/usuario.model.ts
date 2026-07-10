export interface Usuario {
  authenticated: boolean;
  id?: string;
  nombre?: string;
  correo?: string;
  foto?: string;
  biografia?: string;
  redesSociales?: string;
  visibilidadPerfil?: 'PUBLIC' | 'PRIVATE';
  juegoIds?: number[];
  /** Roles asignados al usuario (p. ej. 'ADMIN', 'ORGANIZER', 'PLAYER'). */
  roles?: string[];
}
