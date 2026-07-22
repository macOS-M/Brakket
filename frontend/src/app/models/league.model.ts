/** Liga tal como la devuelve el backend (RF-22, EPIC-07). */
export interface League {
  id: number;
  nombre: string;
  juegoId: number;
  juegoNombre: string;
  /** Arte del juego: portada por defecto cuando la liga no tiene foto. */
  juegoImagenUrl: string | null;
  comisionadoId: number;
  comisionadoNombre: string;
  descripcion: string | null;
  reglas: string | null;
  fotoUrl: string | null;
}

/** Temporada de una liga. */
export interface Season {
  id: number;
  ligaId: number;
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
}

/** Opción de juego para el selector del formulario de liga. */
export interface GameOption {
  id: number;
  nombre: string;
}

/** Cuerpo para crear o configurar una liga. */
export interface LeagueRequest {
  nombre: string;
  juegoId: number;
  descripcion: string | null;
  reglas: string | null;
  fotoUrl: string | null;
}

/** Cuerpo para agregar una temporada. */
export interface SeasonRequest {
  nombre: string;
  fechaInicio: string;
  fechaFin: string;
}
