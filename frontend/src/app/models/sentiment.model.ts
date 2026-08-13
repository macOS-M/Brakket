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

// ---------- Termómetro de sentimiento (RF-40) ----------

/**
 * Estado del termómetro. PENDIENTE es "todavía no hay análisis";
 * INSUFICIENTE es "hay, pero muy poco para mostrar un indicador".
 */
export type EstadoTermometro = 'PENDIENTE' | 'INSUFICIENTE' | 'DISPONIBLE';

/** Reparto de las muestras entre las tres clasificaciones. */
export interface DistribucionSentimiento {
  positivo: number;
  neutro: number;
  negativo: number;
  porcentajePositivo: number;
  porcentajeNeutro: number;
  porcentajeNegativo: number;
}

/** Tramo de la evolución del sentimiento. */
export interface IntervaloSentimiento {
  inicio: string;
  fin: string;
  muestras: number;
  puntajePromedio: number;
  clasificacion: ClasificacionSentimiento;
}

/** Lectura completa del termómetro de una transmisión. */
export interface Termometro {
  transmisionId: number;
  estado: EstadoTermometro;
  /** Texto corto que explica el indicador; siempre viene. */
  resumen: string;
  desde: string | null;
  hasta: string | null;
  intervaloMinutos: number;
  /** Null salvo que el estado sea DISPONIBLE: no hay que pintarlo. */
  puntajeGeneral: number | null;
  clasificacion: ClasificacionSentimiento | null;
  totalMuestras: number;
  minimoMuestras: number;
  distribucion: DistribucionSentimiento;
  intervalos: IntervaloSentimiento[];
}

/** Transmisión con análisis disponible, para el selector del termómetro. */
export interface TransmisionAnalizada {
  id: number;
  estado: string;
  iniciadaEn: string | null;
  torneoId: number | null;
  /** Nombre del torneo; null si la transmisión no cuelga de ninguno. */
  torneoNombre: string | null;
  totalMuestras: number;
}

/** Filtros del termómetro; todos opcionales. */
export interface FiltrosTermometro {
  desde?: string | null;
  hasta?: string | null;
  intervaloMinutos?: number | null;
}

/** Respuesta del asistente del termómetro (RF-40). */
export interface AsistenteRespuesta {
  /** Texto para mostrar; nunca viene vacío. */
  respuesta: string;
  /** false cuando respondió el camino determinista en vez del modelo. */
  generadaPorIa: boolean;
  /** Motivo de la degradación, o null si respondió la IA. */
  aviso: string | null;
}

/** Resultado de pedir una clasificación de sentimiento fuera de cadencia. */
export interface ClasificacionInmediata {
  /** false cuando todavía no había chat acumulado. */
  clasificado: boolean;
  mensajes: number;
  mensaje: string;
}

/** Turno de la conversación con el asistente, para pintar el historial. */
export interface TurnoAsistente {
  autor: 'usuario' | 'asistente';
  texto: string;
  generadaPorIa?: boolean;
  aviso?: string | null;
}
