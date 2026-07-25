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

  // ----- Ajustes personales (RF-18). Datos privados: solo los ve la propia
  // cuenta, nunca el perfil publico. -----

  /** Nombre legal; `nombre` es el visible/gamertag. */
  nombreCompleto?: string;
  /** ISO `YYYY-MM-DD`. */
  fechaNacimiento?: string;
  telefono?: string;
  pais?: string;
  ciudad?: string;
  direccion?: string;
  codigoPostal?: string;
  /** Zona horaria IANA, p. ej. `America/Costa_Rica`. */
  zonaHoraria?: string;
}
