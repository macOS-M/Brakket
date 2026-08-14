export interface Patrocinio {
  id: number;
  patrocinadorId: number;
  patrocinadorNombre: string;
  ligaId: number | null;
  temporadaId: number | null;
  torneoId: number | null;
  condiciones: string | null;
  fechaInicio: string;
  fechaFin: string;
  estado: string;
}

export interface CrearPatrocinioRequest {
  patrocinadorId: number;
  ligaId: number | null;
  temporadaId: number | null;
  torneoId: number | null;
  condiciones: string | null;
  fechaInicio: string;
  fechaFin: string;
}

// TEMPORADA queda en el tipo por compatibilidad con datos viejos, aunque el
// flujo de creación ya no la ofrece (ver association-form). Si prefieres
// quitarla también del tipo, es un cambio de una línea — dímelo.
export type AlcancePatrocinio = 'LIGA' | 'TEMPORADA' | 'TORNEO';
