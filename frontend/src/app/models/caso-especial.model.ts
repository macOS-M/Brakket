/** Los 3 tipos de caso especial que puede registrar el árbitro (RF-28). */
export type TipoCasoEspecial = 'DESCANSO' | 'AVANCE_AUTOMATICO' | 'ABANDONO';

export interface RegistrarCasoEspecialRequest {
  tipo: TipoCasoEspecial;
  justificacion: string | null;
  evidenciaUrl: string | null;
  equipoGanadorId: number | null;
}
