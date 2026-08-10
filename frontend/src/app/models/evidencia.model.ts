export interface AdjuntarEvidenciaRequest {
  url: string;
  descripcion: string | null;
}

export interface EvidenciaResponse {
  id: number;
  disputaId: number;
  subidoPorId: number;
  subidoPorNombre: string;
  url: string;
  descripcion: string | null;
  fechaCreacion: string;
}
