export interface CanalTwitch {
  id: number | null;
  twitchUsuarioId: string | null;
  loginCanal: string | null;
  nombreMostrado: string | null;
  urlCanal: string | null;
  estado: 'SIN_CONFIGURAR' | 'PENDIENTE' | 'ACTIVO' | 'ERROR' | 'NO_DISPONIBLE' | 'REVOCADO';
  activo: boolean;
  ultimoError: string | null;
  ultimaValidacion: string | null;
  credencialesConfiguradas: boolean;
}

export interface TransmisionTwitch {
  id: number;
  twitchStreamId: string | null;
  torneoId: number | null;
  partidaId: number | null;
  estado: string;
  iniciadaEn: string | null;
}

/** Indicadores de audiencia capturados por el muestreo de RF-36. */
export interface MetricasTransmision {
  transmisionId: number;
  estado: string;
  muestras: number;
  pico: number | null;
  promedio: number | null;
  duracionMinutos: number | null;
  iniciadaEn: string | null;
  finalizadaEn: string | null;
  ultimaMuestra: string | null;
}

