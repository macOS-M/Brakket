/** Clasificación cualitativa del sentimiento del chat (RF-39). */
export type ClasificacionSentimiento = 'POSITIVO' | 'NEUTRO' | 'NEGATIVO';

/** Resultado de un análisis de sentimiento del chat. */
export interface SentimientoResultado {
  id: number;
  transmisionId: number | null;
  fechaHora: string;
  clasificacion: ClasificacionSentimiento;
  /** Puntaje en el rango [-100, 100]. */
  puntaje: number;
  /**
   * Mensajes que entraron al motor. Solo viene en la respuesta del análisis:
   * los textos no se persisten, así que al releer la serie llega null.
   */
  mensajesAnalizados: number | null;
  /** Tasa guardada en la métrica de chat; la misma columna que escribe RF-38. */
  mensajesPorMinuto: number;
  usuariosActivos: number;
}

/** Punto de la serie temporal de sentimiento (insumo del termómetro de RF-40). */
export interface PuntoSentimiento {
  fechaHora: string;
  clasificacion: ClasificacionSentimiento;
  puntaje: number;
}

/** Serie de sentimiento de una transmisión. */
export interface SerieSentimiento {
  transmisionId: number;
  ultimo: SentimientoResultado | null;
  promedioPuntaje: number | null;
  totalMuestras: number;
  puntos: PuntoSentimiento[];
}

/** Cuerpo para analizar un lote de mensajes de chat. */
export interface AnalizarChatRequest {
  mensajes: string[];
  usuariosActivos?: number | null;
  /** Segundos que cubre el lote; si no se informa se asume un minuto. */
  ventanaSegundos?: number | null;
}

/** Tope de mensajes por lote; refleja el @Size del DTO del backend. */
export const MAX_MENSAJES_POR_LOTE = 2000;
