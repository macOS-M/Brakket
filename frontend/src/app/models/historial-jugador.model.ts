export interface HistorialEquipoJugador {
  equipoId: number;
  equipoNombre: string;
  juegoId: number | null;
  juegoNombre: string | null;
  juegoIds?: number[];
  juegoNombres?: string[];
  rol: string;
  estado: string;
  fechaIngreso: string;
  fechaSalida: string | null;
}
