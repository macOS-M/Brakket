export type TipoMovimiento = 'ALTA' | 'BAJA' | 'TRANSFERENCIA';

export interface MovimientoPlantilla {
  tipo: TipoMovimiento;
  fecha: string;
  jugadorId: number;
  jugadorNombre: string;
  jugadorPerfilPublico: boolean;
  rol: string | null;
  causaBaja: string | null;
  equipoOrigenId: number | null;
  equipoOrigenNombre: string | null;
  equipoDestinoId: number | null;
  equipoDestinoNombre: string | null;
  responsableId: number | null;
  responsableNombre: string | null;
}

export interface HistorialEquipo {
  equipoId: number;
  equipoNombre: string;
  equipoEstado: string;
  movimientos: MovimientoPlantilla[];
}
