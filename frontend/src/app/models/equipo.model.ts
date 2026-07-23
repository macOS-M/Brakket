export interface Equipo {
  id: number;
  nombre: string;
  logo: string | null;
  /** Banner de portada del perfil (V30, referencia Challenger Mode). */
  bannerUrl: string | null;
  descripcion: string | null;
  sitioWeb: string | null;
  videoUrl: string | null;
  juegoId: number;
  juegoNombre: string;
  capitanId: number;
  capitanNombre: string;
  estado: string;
  estadoPrivacidad: string;
  /** Versión para control de concurrencia optimista; se reenvía en el PUT. */
  version: number;
  redesSociales: string[];
  fechaDisolucion: string | null;
  motivoDisolucion: string | null;
}

/** Resultado público de la búsqueda de equipos (RF-05). */
export interface EquipoBusqueda {
  id: number;
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number | null;
  juegoNombre: string | null;
  disciplina: string | null;
  estado: string;
}

/** Filtros de la búsqueda de equipos (RF-05); todos opcionales. */
export interface BuscarEquiposParams {
  q?: string;
  juegoId?: number;
  disciplina?: string;
  estado?: string;
  page?: number;
  size?: number;
}

/** Página de resultados que devuelve la API en listados paginados. */
export interface Pagina<T> {
  items: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface CrearEquipoRequest {
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  redesSociales: string[];
}

/**
 * RF-02: edición parcial. null/ausente = no tocar el campo;
 * string vacío = borrarlo (logo y descripción).
 */
export interface EditarEquipoRequest {
  nombre?: string | null;
  logo?: string | null;
  bannerUrl?: string | null;
  descripcion?: string | null;
  sitioWeb?: string | null;
  videoUrl?: string | null;
  juegoId?: number | null;
  redesSociales?: string[] | null;
  estadoPrivacidad?: string | null;
  /** Versión leída en el GET; el backend responde 409 si alguien guardó entre medio. */
  version?: number;
}

export interface DisolverEquipoRequest {
  confirmacion: boolean;
  motivo: string | null;
}
