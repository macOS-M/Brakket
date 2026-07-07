export interface Usuario {
  authenticated: boolean;
  id?: string;
  nombre?: string;
  correo?: string;
  foto?: string;
  /** Roles asignados al usuario (p. ej. 'ADMIN', 'ORGANIZER', 'PLAYER'). */
  roles?: string[];
}
