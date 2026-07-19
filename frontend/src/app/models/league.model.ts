/** Liga tal como la devuelve el backend (RF-22, EPIC-07). */
export interface League {
  id: number;
  nombre: string;
  juegoId: number;
  juegoNombre: string;
  comisionadoId: number;
  comisionadoNombre: string;
  activo: boolean;
}

/** Temporada de una liga. */
export interface Season {
  id: number;
  ligaId: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  juegoId: number;
  juegoNombre: string;
  reglas: string;
  estado: SeasonStatus;
  cupoEquipos: number;
  formatoId: number;
  formatoNombre: string;
  version: number;
  mensaje?: string;
}

export type SeasonStatus = 'PLANIFICADA' | 'ACTIVA' | 'FINALIZADA' | 'CANCELADA';

export interface FormatOption { id: number; nombre: string; }

/** Opción de juego para el selector del formulario de liga. */
export interface GameOption {
  id: number;
  nombre: string;
}

/** Cuerpo para crear o configurar una liga. */
export interface LeagueRequest {
  nombre: string;
  juegoId: number;
}

/** Cuerpo para agregar una temporada. */
export interface SeasonRequest {
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
  reglas: string;
  estado: SeasonStatus;
  cupoEquipos: number;
  formatoId: number;
}

export interface UpdateSeasonRequest { configuracion: SeasonRequest; version: number; }
