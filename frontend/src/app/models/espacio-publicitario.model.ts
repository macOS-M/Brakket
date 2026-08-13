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

// Rediseño: de 5 a 3. Se retiran DASHBOARD_CARD y CALENDARIO_FRANJA — son
// pantallas que agregan contenido de muchas ligas/torneos a la vez, sin un
// "dueño" natural del espacio (ver migración V64).
export const UBICACIONES_ESPACIO = [
  'TRANSMISION_INFERIOR',
  'TORNEO_CABECERA',
  'LIGA_CABECERA'
] as const;

export type UbicacionEspacio = typeof UBICACIONES_ESPACIO[number];
