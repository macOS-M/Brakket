export interface Equipo {
  id: number;
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  juegoNombre: string;
  capitanId: number;
  capitanNombre: string;
  estado: string;
  estadoPrivacidad: string;
  redesSociales: string[];
}

export interface CrearEquipoRequest {
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  redesSociales: string[];
}

/**
 * RF-02: edición parcial. Todos los campos opcionales; el backend
 * solo actualiza los que viajan con valor.
 */
export interface EditarEquipoRequest {
  nombre?: string | null;
  logo?: string | null;
  descripcion?: string | null;
  juegoId?: number | null;
  redesSociales?: string[] | null;
  estadoPrivacidad?: string | null;
}
