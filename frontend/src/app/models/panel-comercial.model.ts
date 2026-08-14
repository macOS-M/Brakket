export interface PatrocinioResumen {
  patrocinioId: number;
  estado: string;
  vencido: boolean;
  ligaId: number | null;
  temporadaId: number | null;
  torneoId: number | null;
  fechaInicio: string;
  fechaFin: string;
  cantidadEspacios: number;
}

export interface PanelComercial {
  patrocinadorId: number;
  patrocinadorNombre: string;
  patrocinios: PatrocinioResumen[];
}

export interface MetricasPatrocinio {
  patrocinioId: number;
  transmisionId: number | null;
  espectadoresPromedio: number | null;
  picoEspectadores: number | null;
  mensajesPorMinutoPromedio: number | null;
  sentimientoPredominante: string | null;
  sentimientoPendiente: boolean;
}
