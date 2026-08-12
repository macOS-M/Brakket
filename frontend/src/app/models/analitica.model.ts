/** RF-37: consulta de métricas de una transmisión por período y rango horario. */

export type AgrupacionMetricas = 'CRUDA' | 'HORA';

export type ClaveSerie =
  | 'ESPECTADORES'
  | 'MENSAJES_POR_MINUTO'
  | 'USUARIOS_ACTIVOS'
  | 'SENTIMIENTO';

/** `valor` nulo es un hueco de muestreo, no un cero. */
export interface PuntoSerie {
  instante: string;
  valor: number | null;
}

export interface SerieMetrica {
  clave: ClaveSerie;
  etiqueta: string;
  unidad: string | null;
  /** 0 significa que la serie todavía no tiene datos (p. ej. sentimiento antes de RF-39). */
  muestras: number;
  promedio: number | null;
  pico: number | null;
  minimo: number | null;
  puntos: PuntoSerie[];
}

export interface ResumenMetricas {
  muestrasAudiencia: number;
  picoEspectadores: number | null;
  promedioEspectadores: number | null;
  muestrasChat: number;
  promedioMensajesPorMinuto: number | null;
  picoUsuariosActivos: number | null;
  muestrasSentimiento: number;
  promedioPuntaje: number | null;
  clasificacionPredominante: string | null;
}

export interface SeriesTransmision {
  transmisionId: number;
  etiquetaTransmision: string;
  estado: string;
  agrupacion: AgrupacionMetricas;
  desde: string | null;
  hasta: string | null;
  duracionMinutos: number | null;
  /** Cadencia del muestreo; con esto se detectan los huecos al graficar. */
  intervaloSegundos: number | null;
  origen: 'REAL' | 'SIMULADO' | 'MIXTO' | null;
  resumen: ResumenMetricas;
  /** Siempre las cuatro claves; una serie sin datos viene con `puntos` vacío. */
  series: SerieMetrica[];
}

export interface TransmisionAnalizable {
  id: number;
  etiqueta: string;
  torneoId: number | null;
  nombreTorneo: string | null;
  estado: string;
  iniciadaEn: string | null;
  finalizadaEn: string | null;
  muestras: number;
}
