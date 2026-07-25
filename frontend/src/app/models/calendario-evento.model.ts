export interface CalendarioEvento {
  torneoId: number;
  nombre: string;
  juegoId: number;
  juegoNombre: string;
  ligaId: number | null;
  ligaNombre: string | null;
  temporadaId: number | null;
  fechaInicio: string;
  fechaFin: string | null;
  estado: string;
  publico: boolean;
}
