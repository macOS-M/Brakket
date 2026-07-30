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
}
