export interface ImpugnarResultadoRequest {
  motivo: string;
  descripcion: string;
  evidenciaUrl: string | null;
}

export interface DisputaResponse {
  id: number;
  partidaId: number;
  levantadaPorId: number;
  levantadaPorNombre: string;
  motivo: string;
  descripcion: string;
  evidenciaUrl: string | null;
  estado: string;
  fechaCreacion: string;
  decision: string | null;
  justificacionResolucion: string | null;
  sancion: string | null;
  resueltaPorNombre: string | null;
  fechaResolucion: string | null;
}

export type DecisionDisputa = 'MANTENER' | 'REVERTIR';

export interface ResolverDisputaRequest {
  decision: DecisionDisputa;
  justificacion: string;
  sancion: string | null;
  equipoGanadorId: number | null;
}

export interface ApelarRequest {
  motivo: string;
}

export interface ResolverApelacionRequest {
  decisionFinal: string | null;
  equipoGanadorId: number | null;
}

export interface ApelacionResponse {
  id: number;
  disputaId: number;
  apeladaPorId: number | null;
  apeladaPorNombre: string | null;
  motivo: string;
  estado: string;
  decisionFinal: string | null;
  comisionadoNombre: string | null;
  fechaCreacion: string;
  fechaResolucion: string | null;
}
