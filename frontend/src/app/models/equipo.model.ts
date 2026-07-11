export interface Equipo {
  id: number;
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  estado: string;
  fechaDisolucion: string | null;
  motivoDisolucion: string | null;
}

export interface DisolverEquipoRequest {
  confirmacion: boolean;
  motivo: string | null;
}
