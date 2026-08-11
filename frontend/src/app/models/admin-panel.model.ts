/** Conteos globales de la plataforma para el panel de administración (RF-49). */
export interface ResumenPlataforma {
  usuarios: number;
  usuariosBloqueados: number;
  equipos: number;
  juegos: number;
  juegosActivos: number;
  ligas: number;
  torneos: number;
  torneosEnCurso: number;
  transmisionesActivas: number;
  disputas: number;
}

/** Entrada del log de auditoría (actividad reciente). */
export interface LogAuditoriaEntry {
  id: number;
  fecha: string;
  accion: string;
  entidad: string;
  entidadId: number;
  actorNombre: string | null;
  actorCorreo: string | null;
}

/** Carga completa del panel global: resumen + actividad reciente. */
export interface PanelGlobal {
  resumen: ResumenPlataforma;
  actividadReciente: LogAuditoriaEntry[];
}
