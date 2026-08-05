export interface Patrocinio {
  id: number;
  patrocinadorId: number;
  patrocinadorNombre: string;
  ligaId: number | null;
  temporadaId: number | null;
  torneoId: number | null;
  nivel: string;
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
  nivel: string;
  condiciones: string | null;
  fechaInicio: string;
  fechaFin: string;
}

export type AlcancePatrocinio = 'LIGA' | 'TEMPORADA' | 'TORNEO';

export const NIVELES_PATROCINIO = ['ORO', 'PLATA', 'BRONCE'] as const;
