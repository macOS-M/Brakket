export type TipoReporte = 'COMPETENCIA' | 'AUDIENCIA' | 'PATROCINIO' | 'ESTADISTICA';

export interface FiltrosReporte {
  tipo: TipoReporte;
  torneoId?: number | null;
  patrocinadorId?: number | null;
  desde?: string;
  hasta?: string;
}

export interface ReporteResponse {
  tipo: TipoReporte;
  titulo: string;
  fechaGeneracion: string;
  usuarioSolicitante: string;
  filtrosDescripcion: string;
  columnas: string[];
  filas: string[][];
}
