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

