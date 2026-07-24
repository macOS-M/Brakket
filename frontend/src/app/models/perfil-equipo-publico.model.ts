export interface IntegrantePublico {
  usuarioId: number;
  nombre: string;
  rol: string;
  fechaUnion: string;
}

export interface TorneoRelacionado {
  id: number;
  nombre: string;
  estado: string;
  fechaInicio: string;
  fechaFin: string;
  estadoInscripcion: string;
}

export interface EstadisticasGeneralesEquipo {
  victorias: number;
  derrotas: number;
  torneosJugados: number;
  disponibles: boolean;
}

export interface PerfilEquipoPublico {
  id: number;
  nombre: string;
  logo: string | null;
  bannerUrl: string | null;
  descripcion: string | null;
  sitioWeb: string | null;
  videoUrl: string | null;
  estado: string;
  capitanId: number;
  juegoId: number | null;
  juegoNombre: string | null;
  redesSociales: string[];
  plantilla: IntegrantePublico[];
  torneos: TorneoRelacionado[];
  estadisticas: EstadisticasGeneralesEquipo;
}

/** Fila del listado público: solo lo que la lista muestra. */
export interface EquipoResumenPublico {
  id: number;
  nombre: string;
  logo: string | null;
  juegoNombre: string | null;
  integrantesActivos: number;
}
