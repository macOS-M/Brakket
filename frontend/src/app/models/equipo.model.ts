export interface Equipo {
  id: number;
  nombre: string;
  logo: string | null;
  descripcion: string | null;
  juegoId: number;
  juegoNombre: string;
  capitanId: number;
  capitanNombre: string;
  redesSociales: string[];
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
