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
  estado: string;
  fechaDisolucion: string | null;
  motivoDisolucion: string | null;
}

export interface CrearEquipoRequest {
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  redesSociales: string[];
}

export interface DisolverEquipoRequest {
  confirmacion: boolean;
  motivo: string | null;
}
