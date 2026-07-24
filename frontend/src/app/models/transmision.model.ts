/** Respuesta de GET /api/transmisiones (RF-35). */
export interface TransmisionesRespuesta {
  transmisiones: Transmision[];
  /**
   * Hora de la última consulta EXITOSA a la plataforma. Con degradado=true
   * la UI la usa para avisar "última información de las HH:mm" en vez de
   * presentar datos viejos como actuales (RNF-15).
   */
  actualizadoEn: string | null;
  degradado: boolean;
}

export interface Transmision {
  plataforma: 'TWITCH' | 'YOUTUBE' | 'KICK';
  loginCanal: string;
  nombreCanal: string;
  avatarUrl: string | null;
  urlCanal: string;
  estado: 'EN_VIVO' | 'OFFLINE' | 'DESCONOCIDO';
  titulo: string | null;
  espectadores: number | null;
  thumbnailUrl: string | null;
  categoria: string | null;
  idioma: string | null;
  iniciadaEn: string | null;
  destacada: boolean;
  torneoId: number | null;
  nombreTorneo: string | null;
  vod: VodTransmision | null;
}

export interface VodTransmision {
  id: string;
  url: string;
  titulo: string;
  thumbnailUrl: string | null;
  duracion: string | null;
  publicadoEn: string | null;
}

/**
 * Tarjeta de la grilla. "Próximamente" es relleno visual de los huecos:
 * existe SOLO en el frontend (nunca en el enum de estado del dominio) y se
 * genera desde esta misma estructura, no como markup estático.
 */
export type TarjetaTransmision =
  | { tipo: 'real'; transmision: Transmision }
  | { tipo: 'proximamente' };
