export interface Equipo {
  id: number;
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  juegoNombre: string;
  capitanId: number;
  capitanNombre: string;
  redesSociales: string[];
}

export interface CrearEquipoRequest {
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  redesSociales: string[];
}

export interface ActualizarEquipoRequest extends CrearEquipoRequest {
  version: number;
}
