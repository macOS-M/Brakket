export interface PerfilCompetitivoRequest {
  juegoId: number;
  modalidad: 'INDIVIDUAL' | 'EQUIPOS';
  plantillaMinima: number;
  plantillaMaxima: number;
  formatosIds: number[];
  estadisticasIds: number[];
}


export interface PerfilCompetitivoResponse {
  id: number;
  juegoId: number;
  juego: string;
  modalidad: string;
  plantillaMinima: number;
  plantillaMaxima: number;
  formatos: string[];
  estadisticas: string[];
  formatosIds: number[];
  estadisticasIds: number[];
  activo: boolean;
  mensaje?: string | null;
}

export interface CatalogoCompetitivo {
  id: number;
  nombre: string;
  obligatorio: boolean;
}
