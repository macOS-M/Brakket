export interface EspacioPublicitario {
  id: number;
  patrocinioId: number;
  patrocinadorNombre: string;
  ubicacion: string;
  imagenUrl: string;
  enlaceUrl: string | null;
  estado: string;
  fechaInicio: string;
  fechaFin: string;
}

export interface CrearEspacioPublicitarioRequest {
  patrocinioId: number;
  ubicacion: string;
  imagenUrl: string;
  enlaceUrl: string | null;
}

export const UBICACIONES_ESPACIO = [
  'TRANSMISION_INFERIOR',
  'TORNEO_CABECERA',
  'LIGA_CABECERA',
  'DASHBOARD_CARD',
  'CALENDARIO_FRANJA'
] as const;

export type UbicacionEspacio = typeof UBICACIONES_ESPACIO[number];
